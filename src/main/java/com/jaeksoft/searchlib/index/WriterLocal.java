/*
 * License Agreement for OpenSearchServer
 * <p>
 * Copyright (C) 2008-2017 Emmanuel Keller / Jaeksoft
 * <p>
 * http://www.open-search-server.com
 * <p>
 * This file is part of OpenSearchServer.
 * <p>
 * OpenSearchServer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * OpenSearchServer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with OpenSearchServer.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.jaeksoft.searchlib.index;

import com.jaeksoft.searchlib.Logging;
import com.jaeksoft.searchlib.SearchLibException;
import com.jaeksoft.searchlib.SearchLibException.UniqueKeyMissing;
import com.jaeksoft.searchlib.analysis.AbstractAnalyzer;
import com.jaeksoft.searchlib.analysis.Analyzer;
import com.jaeksoft.searchlib.analysis.CompiledAnalyzer;
import com.jaeksoft.searchlib.analysis.IndexDocumentAnalyzer;
import com.jaeksoft.searchlib.analysis.LanguageEnum;
import com.jaeksoft.searchlib.analysis.PerFieldAnalyzer;
import com.jaeksoft.searchlib.request.AbstractRequest;
import com.jaeksoft.searchlib.schema.FieldValueItem;
import com.jaeksoft.searchlib.schema.Schema;
import com.jaeksoft.searchlib.schema.SchemaField;
import com.jaeksoft.searchlib.schema.SchemaFieldList;
import com.jaeksoft.searchlib.util.IOUtils;
import com.jaeksoft.searchlib.webservice.query.document.IndexDocumentResult;
import com.jaeksoft.searchlib.webservice.query.document.IndexDocumentResult.IndexField;
import com.jaeksoft.searchlib.webservice.query.document.IndexDocumentResult.IndexTerm;
import org.apache.commons.collections.CollectionUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.index.SnapshotDeletionPolicy;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

public class WriterLocal extends WriterAbstract {

	private final IndexDirectory indexDirectory;
	private final ReentrantLock indexWriterLock;

	protected WriterLocal(IndexConfig indexConfig, IndexDirectory indexDirectory,
			IndexDirectory snapshotDeletionDirectory) throws IOException {
		super(indexConfig);
		this.indexDirectory = indexDirectory;
		indexWriterLock = new ReentrantLock();
	}

	private void close(IndexWriter indexWriter) {
		if (indexWriter == null)
			return;
		try {
			indexWriter.close();
		} catch (Exception e) {
			Logging.warn(e);
		} finally {
			indexDirectory.unlock();
			indexWriterLock.unlock();
		}
	}

	public final void create() throws IOException, SearchLibException {
		IndexWriter indexWriter = null;
		try {
			indexWriter = open(true);
		} finally {
			close(indexWriter);
		}
	}

	private IndexWriter open(boolean create) throws IOException, SearchLibException {
		indexWriterLock.lock();
		final IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_36, null);
		config.setOpenMode(create ? OpenMode.CREATE_OR_APPEND : OpenMode.APPEND);
		config.setMergeScheduler(new SerialMergeScheduler());
		config.setWriteLockTimeout(indexConfig.getWriteLockTimeout());
		config.setRAMBufferSizeMB(128);
		final Similarity similarity = indexConfig.getNewSimilarityInstance();
		if (similarity != null)
			config.setSimilarity(similarity);
		if (!create) {
			final SnapshotDeletionPolicy snapshotDeletionPolicy =
					new SnapshotDeletionPolicy(config.getIndexDeletionPolicy());
			config.setIndexDeletionPolicy(snapshotDeletionPolicy);
		}
		Logging.debug("WriteLocal open " + indexDirectory.getDirectory());
		return new IndexWriter(indexDirectory.getDirectory(), config);
	}

	private IndexWriter open() throws IOException, SearchLibException {
		return open(false);
	}

	@Deprecated
	public void addDocument(Document document) throws IOException, SearchLibException {
		IndexWriter indexWriter = null;
		try {
			indexWriter = open();
			indexWriter.addDocument(document);
		} finally {
			close(indexWriter);
		}
	}

	private boolean updateDocNoLock(SchemaField uniqueField, IndexWriter indexWriter, Schema schema,
			IndexDocument document) throws IOException, NoSuchAlgorithmException, SearchLibException {
		if (!acceptDocument(document))
			return false;

		Document doc = getLuceneDocument(schema, document);
		PerFieldAnalyzer pfa = schema.getIndexPerFieldAnalyzer(document.getLang());

		updateDocNoLock(uniqueField, indexWriter, pfa, doc);
		return true;
	}

	private void updateDocNoLock(SchemaField uniqueField, IndexWriter indexWriter, AbstractAnalyzer analyzer,
			Document doc) throws UniqueKeyMissing, CorruptIndexException, IOException {
		if (uniqueField != null) {
			String uniqueFieldName = uniqueField.getName();
			String uniqueFieldValue = doc.get(uniqueFieldName);
			if (uniqueFieldValue == null)
				throw new UniqueKeyMissing(uniqueFieldName);
			indexWriter.updateDocument(new Term(uniqueFieldName, uniqueFieldValue), doc, analyzer);
		} else
			indexWriter.addDocument(doc, analyzer);
	}

	@Override
	public boolean updateDocument(Schema schema, IndexDocument document) throws SearchLibException {
		IndexWriter indexWriter = null;
		try {
			indexWriter = open();
			SchemaField uniqueField = schema.getFieldList().getUniqueField();
			boolean updated = updateDocNoLock(uniqueField, indexWriter, schema, document);
			close(indexWriter);
			indexWriter = null;
			return updated;
		} catch (IOException | NoSuchAlgorithmException e) {
			throw new SearchLibException(e);
		} finally {
			close(indexWriter);
		}
	}

	@Override
	public int updateDocuments(Schema schema, Collection<IndexDocument> documents) throws SearchLibException {
		IndexWriter indexWriter = null;
		try {
			final AtomicInteger count = new AtomicInteger();
			final IndexWriter iw = indexWriter = open();
			final SchemaField uniqueField = schema.getFieldList().getUniqueField();

			final AtomicReference<Exception> exceptionReference = new AtomicReference<>();
			final ExecutorService executorService =
					Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

			try {
				for (IndexDocument document : documents) {
					executorService.submit(() -> {
						try {
							if (updateDocNoLock(uniqueField, iw, schema, document))
								count.incrementAndGet();
						} catch (IOException | NoSuchAlgorithmException | SearchLibException e) {
							exceptionReference.weakCompareAndSet(null, e);
						}
					});
				}

			} finally {
				executorService.shutdown();
			}
			executorService.awaitTermination(1, TimeUnit.HOURS);
			if (exceptionReference.get() != null)
				throw SearchLibException.newInstance(exceptionReference.get());

			close(indexWriter);
			indexWriter = null;
			return count.get();
		} catch (IOException | InterruptedException e) {
			throw new SearchLibException(e);
		} finally {
			close(indexWriter);
		}
	}

	@Override
	public int updateIndexDocuments(Schema schema, Collection<IndexDocumentResult> documents)
			throws SearchLibException {
		IndexWriter indexWriter = null;
		try {
			final AtomicInteger count = new AtomicInteger();
			final IndexWriter iw = indexWriter = open();
			final SchemaField uniqueField = schema.getFieldList().getUniqueField();

			final AtomicReference<Exception> exceptionReference = new AtomicReference<>();
			final ExecutorService executorService =
					Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

			try {
				for (IndexDocumentResult document : documents) {
					executorService.submit(() -> {
						final Document doc = getLuceneDocument(schema, document);
						final IndexDocumentAnalyzer analyzer = new IndexDocumentAnalyzer(document);
						try {
							updateDocNoLock(uniqueField, iw, analyzer, doc);
							count.incrementAndGet();
						} catch (IOException | SearchLibException e) {
							exceptionReference.weakCompareAndSet(null, e);
						}
					});
				}
			} finally {
				executorService.shutdown();
			}
			executorService.awaitTermination(1, TimeUnit.HOURS);
			if (exceptionReference.get() != null)
				throw SearchLibException.newInstance(exceptionReference.get());

			close(indexWriter);
			indexWriter = null;
			return count.get();
		} catch (IOException | InterruptedException e) {
			throw new SearchLibException(e);
		} finally {
			close(indexWriter);
		}
	}

	private static Document getLuceneDocument(Schema schema, IndexDocument document)
			throws IOException, SearchLibException {
		schema.getIndexPerFieldAnalyzer(document.getLang());
		Document doc = new Document();
		LanguageEnum lang = document.getLang();
		SchemaFieldList schemaFieldList = schema.getFieldList();
		for (FieldContent fieldContent : document) {
			if (fieldContent == null)
				continue;
			String fieldName = fieldContent.getField();
			SchemaField field = schemaFieldList.get(fieldName);
			if (field == null)
				continue;
			Analyzer analyzer = schema.getAnalyzer(field, lang);
			@SuppressWarnings("resource") CompiledAnalyzer compiledAnalyzer =
					(analyzer == null) ? null : analyzer.getIndexAnalyzer();
			List<FieldValueItem> valueItems = fieldContent.getValues();
			if (valueItems == null)
				continue;
			for (FieldValueItem valueItem : valueItems) {
				if (valueItem == null)
					continue;
				String value = valueItem.getValue();
				if (value == null)
					continue;
				if (compiledAnalyzer != null)
					if (!compiledAnalyzer.isAnyToken(fieldName, value))
						continue;
				doc.add(field.getLuceneField(value, valueItem.getBoost()));
			}
		}
		return doc;
	}

	private static Document getLuceneDocument(Schema schema, IndexDocumentResult document) {
		if (CollectionUtils.isEmpty(document.fields))
			return null;
		SchemaFieldList schemaFieldList = schema.getFieldList();
		Document doc = new Document();
		for (IndexField indexField : document.fields) {
			SchemaField field = schemaFieldList.get(indexField.field);
			if (field == null)
				continue;
			if (indexField.stored != null) {
				for (String value : indexField.stored)
					doc.add(field.getLuceneField(value, null));
			} else {
				if (indexField.terms != null)
					for (IndexTerm term : indexField.terms)
						doc.add(field.getLuceneField(term.t, null));
			}
		}
		return doc;
	}

	public int deleteDocuments(int[] ids) throws IOException, SearchLibException {
		if (ids == null || ids.length == 0)
			return 0;
		IndexReader indexReader = null;
		try {
			int l = 0;
			indexReader = IndexReader.open(indexDirectory.getDirectory(), false);
			for (int id : ids)
				if (!indexReader.isDeleted(id)) {
					indexReader.deleteDocument(id);
					l++;
				}
			indexReader.close();
			indexReader = null;
			return l;
		} finally {
			IOUtils.close(indexReader);
		}
	}

	@Override
	public void deleteAll() throws SearchLibException {
		IndexWriter indexWriter = null;
		try {
			indexWriter = open();
			indexWriter.deleteAll();
			close(indexWriter);
			indexWriter = null;
		} catch (IOException e) {
			throw new SearchLibException(e);
		} finally {
			close(indexWriter);
		}
	}

	@Override
	public int deleteDocuments(AbstractRequest request) throws SearchLibException {
		throw new SearchLibException("WriterLocal.deleteDocument(request) is not implemented");
	}

	private void mergeNoLock(IndexDirectory directory) throws SearchLibException {
		IndexWriter indexWriter = null;
		try {
			indexWriter = open();
			indexWriter.addIndexes(directory.getDirectory());
			close(indexWriter);
			indexWriter = null;
		} catch (IOException e) {
			throw new SearchLibException(e);
		} finally {
			close(indexWriter);
		}

	}

	@Override
	public void mergeData(WriterInterface source) throws SearchLibException {
		WriterLocal sourceWriter;
		if (!(source instanceof WriterLocal))
			throw new SearchLibException("Unsupported operation");
		sourceWriter = (WriterLocal) source;
		try {
			sourceWriter.setMergingSource(true);
			setMergingTarget(true);
			mergeNoLock(sourceWriter.indexDirectory);
		} finally {
			if (sourceWriter != null)
				sourceWriter.setMergingSource(false);
			setMergingTarget(false);
		}
	}
}

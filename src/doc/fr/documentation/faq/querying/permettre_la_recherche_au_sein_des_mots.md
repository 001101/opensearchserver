Si des documents index�s contiennent le mot "ultraviolet" on peut souhaiter renvoyer ces documents lorsque la recherche est faite sur "ultra" ou bien sur "violet".

Cela peut �tre r�alis� en cr�ant un nouvel "analyzer" et en l'appliquant � un nouveau champ du sch�ma.

L'analyzer doit utiliser le "Ngram filter" pour cr�er plusieurs mots � partir de mots existant :
![ngram](ngram.png)

Un nouveau champ doit ensuite �tre cr��. Il copiera la valeur du champ existant `content` et utilisera le nouveau `NgramAnalyzer`. 

![ngram field](ngram_field.png)

Une fois les documents r�-index�s et le template de query modifi� pour inclure la recherche dans le nouveau champ `mega_content` les documents contenant le mot "ultraviolet" seront renvoy�s lors de la recherche de "ultra" ou de "violet".
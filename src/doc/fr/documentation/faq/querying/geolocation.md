OpenSearchServer offre la possibilit� de r�aliser des requ�tes g�olocalis�es. A partir du moment o� les coordonnn�es (longitude et latitude) ont �t� enregistr�es avec les documents index�s il est possible de requ�ter les documents se trouvant dans un rectangle g�ographique donn�.

## Indexer les documents en int�grant les informations de g�olocalisation
### Pr�parer le sch�ma

Il faut commencer par fournir la latitude et la longitude de chaque document.

Le sch�ma doit comporter deux champs pour stocker cette information:
- latitude (index�)
- longitude (index�)

L'analyzer correspondant au [syst�me de coordonn�es](http://fr.wikipedia.org/wiki/Coordonn%C3%A9es_g%C3%A9ographiques) appropri� doit �tre choisi:

- GeoRadianAnalyzer
- GeoDegreesAnalyzer

![Alt text](geo_fields.png)

### Indexer les donn�es

Les coordonn�es (latitude et longitude) doivent �tre exprim�s au format d�cimal. Par exemple :

- Pour des degr�s: -52.090904
- Pour des radians: -0.675757575575

#### Utiliser l'API : ajouter deux champs au JSON

L'API [d'indexation JSON](https://github.com/jaeksoft/opensearchserver/wiki/Document-put-JSON) est document�e sur notre [wiki des API](https://github.com/jaeksoft/opensearchserver/wiki/).

    [
      {
        "lang": "ENGLISH",
        "fields": [
          { "name": "city", "value": "New-York" },
          { "name": "latitude", "value": 40.7142700 },
          { "name": "longitude", "value": -74.0059700 }
        ]
      },
      {
        "lang": "FRENCH",
        "fields": [
          { "name": "city", "value": "Paris" },
          { "name": "latitude", "value": 48.8534100 },
          { "name": "longitude", "value": 2.3488000 }
         ]
       },
       {
        "lang": "GERMAN",
        "fields": [
          { "name": "city", "value": "Berlin" },
          { "name": "latitude", "value": 52.5243700 },
          { "name": "longitude", "value": 13.4105300 }
         ]
       }
    ] 

### Requ�ter les donn�es

Recherchons par exemple les villes situ�es � moins de 10 kilom�tres d'un point donn� (appel� "la position centrale").

Les champs latitudeField et longitudeField doivent �tre mapp�s aux champs du sch�ma contenant ces informations.

Le tableau "geo" permet de d�finir la position centrale.

Le filtre "GeoFilter" applique un filtre g�ographique selon les param�tres donn�s.

La requ�te est r�alis�e gr�ce � [l'API Search(field)](https://github.com/jaeksoft/opensearchserver/wiki/Search-field) d�crite sur [notre wiki](https://github.com/jaeksoft/opensearchserver/wiki/) :

    {
        "start": 0,
        "rows": 10,
        "geo": {
            "latitudeField": "latitude",
            "longitudeField": "longitude",
            "latitude": 48.85341,
            "longitude": 2.3488,
            "coordUnit": "DEGREES"
        },
        "emptyReturnsAll": true,
        "filters": [
            {
                "type": "GeoFilter",
                "shape": "SQUARED",
                "negative": false,
                "unit": "KILOMETERS",
                "distance": 10
            }
        ],
        "returnedFields": [ "city", "latitude", "longitude" ]
    }

### Documents retourn�s

Voici les r�sultats :

    {
    "successful": true,
    "documents": [
        {
            "pos": 0,
            "score": 1,
            "collapseCount": 0,
            "fields": [
                {
                    "fieldName": "city",
                    "values": [
                        "Paris"
                    ]
                },
                {
                    "fieldName": "latitude",
                    "values": [
                        "P0.8526529"
                    ]
                },
                {
                    "fieldName": "longitude",
                    "values": [
                        "P0.0409943"
                    ]
                }
            ]
        }
    ],
    "facets": [],
    "query": "*:*",
    "rows": 10,
    "start": 0,
    "numFound": 1,
    "time": 126,
    "collapsedDocCount": 0,
    "maxScore": 1
    }

### Ajouter la distance dans les r�sultats

Pour retourner la distance entre la position centrale et chacun des r�sultats il est n�cessaire d'ajouter un tableau "scorings" dans la requ�te :

    {
        "start": 0,
        "rows": 10,
        "geo": {
            "latitudeField": "latitude",
            "longitudeField": "longitude",
            "latitude": 48.85341,
            "longitude": 2.3488,
            "coordUnit": "DEGREES"
        },
        "emptyReturnsAll": true,
        "filters": [
            {
                "type": "GeoFilter",
                "shape": "SQUARED",
                "negative": false,
                "unit": "KILOMETERS",
                "distance": 10
            }
        ],
        "returnedFields": [ "city", "latitude", "longitude" ],
        "scorings": [
            {
                "ascending": false,
                "weight": 1,
                "type": "DISTANCE"
            }
        ]
    }


Voici les r�sultats :

    {
    "successful": true,
    "documents": [
        {
            "pos": 0,
            "score": 1,
            "distance": 0.00033325536,
            "collapseCount": 0,
            "fields": [
                {
                    "fieldName": "city",
                    "values": [
                        "Paris"
                    ]
                },
                {
                    "fieldName": "latitude",
                    "values": [
                        "P0.8526529"
                    ]
                },
                {
                    "fieldName": "longitude",
                    "values": [
                        "P0.0409943"
                    ]
                }
            ]
        }
    ],
    "facets": [],
    "query": "*:*",
    "rows": 10,
    "start": 0,
    "numFound": 1,
    "time": 0,
    "collapsedDocCount": 0,
    "maxScore": 0
    }
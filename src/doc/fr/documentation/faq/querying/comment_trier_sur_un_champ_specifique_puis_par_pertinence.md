Les documents peuvent facilement �tre tri�s sur un champ sp�cifique, par exemple le champ `price` :

    {
      "query" : "phone",
      "start" : 0,
      "rows"  : 10,
      "sorts": [
        {
          "field": "price",
          "direction": "ASC"
        }
      ]
    }

Mais les documents avec la m�me valeur pour le champ `price` semblent ensuite �tre tri�s al�atoirement.

Un second niveau de tri peut �tre ajout�, sur le champ `score`. `score` n'est pas un vrai champ du sch�ma mais c'est une information qui peut �tre utilis�e au moment de la query pour trier les documents selon leur pertinence par rapport � la requ�te :

    {
      "query" : "phone",
      "start" : 0,
      "rows"  : 10,
      "sorts": [
        {
          "field": "price",
          "direction": "ASC"
        },
        {
          "field": "score",
          "direction": "DESC"
        }
      ]
    }
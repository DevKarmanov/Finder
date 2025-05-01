package karm.van.repo.elasticRepo;

import karm.van.model.CardDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface ElasticRepo extends ElasticsearchRepository<CardDocument, Long> {

    @Query("""
    {
      "bool": {
        "should": [
          {
            "multi_match": {
              "query": "?0",
              "fields": ["title^4", "text^3"],
              "type": "best_fields",
              "operator": "or",
              "fuzziness": "AUTO",
              "prefix_length": 2
            }
          },
          {
            "match_phrase": {
              "title": {
                "query": "?0",
                "slop": 2,
                "boost": 3
              }
            }
          },
          {
            "match_phrase": {
              "text": {
                "query": "?0",
                "slop": 2,
                "boost": 2
              }
            }
          },
          {
            "fuzzy": {
              "title": {
                "value": "?0",
                "fuzziness": 1,
                "boost": 1.5
              }
            }
          }
        ],
        "filter": [
          {
            "range": {
              "createTime": {
                "gte": "?1"
              }
            }
          },
          {
            "terms": {
              "tags": ?2
            }
          }
        ]
      }
    }
    """)
    Page<CardDocument> findByQueryWithFilters(String query, String date, List<String> tags, Pageable pageable);

    @Query("""
{
  "bool": {
    "filter": [
      {
        "range": {
          "createTime": {
            "gte": "?0"
          }
        }
      },
      {
        "terms": {
          "tags": ?1
        }
      }
    ]
  }
}
""")
    Page<CardDocument> findByFiltersOnly(String date, List<String> tags, Pageable pageable);

    @Query("""
{
  "bool": {
    "filter": [
      {
        "range": {
          "createTime": {
            "gte": "?0"
          }
        }
      }
    ]
  }
}
""")
    Page<CardDocument> findByDateOnly(String date, Pageable pageable);

    @Query("""
{
  "bool": {
    "filter": [
      {
        "terms": {
          "tags": ?0
        }
      }
    ]
  }
}
""")
    Page<CardDocument> findByTagsOnly(List<String> tags, Pageable pageable);

    @Query("""
    {
      "bool": {
        "should": [
          {
            "multi_match": {
              "query": "?0",
              "fields": ["title^4", "text^3"],
              "type": "best_fields",
              "operator": "or",
              "fuzziness": "AUTO",
              "prefix_length": 2
            }
          },
          {
            "match_phrase": {
              "title": {
                "query": "?0",
                "slop": 2,
                "boost": 3
              }
            }
          },
          {
            "match_phrase": {
              "text": {
                "query": "?0",
                "slop": 2,
                "boost": 2
              }
            }
          },
          {
            "fuzzy": {
              "title": {
                "value": "?0",
                "fuzziness": 1,
                "boost": 1.5
              }
            }
          }
        ]
      }
    }
    """)
    Page<CardDocument> findByQueryOnly(String query, Pageable pageable);

    @Query("""
{
  "bool": {
    "must": [
      {
        "multi_match": {
          "query": "?0",
          "fields": ["title^4", "text^3"],
          "type": "best_fields",
          "operator": "or",
          "fuzziness": "AUTO",
          "prefix_length": 2
        }
      },
      {
        "range": {
          "createTime": {
            "gte": "?1"
          }
        }
      }
    ],
    "should": [
      {
        "match_phrase": {
          "title": {
            "query": "?0",
            "slop": 2,
            "boost": 3
          }
        }
      },
      {
        "match_phrase": {
          "text": {
            "query": "?0",
            "slop": 2,
            "boost": 2
          }
        }
      },
      {
        "fuzzy": {
          "title": {
            "value": "?0",
            "fuzziness": 1,
            "boost": 1.5
          }
        }
      }
    ]
  }
}
""")
    Page<CardDocument> findByQueryAndDateOnly(String query, String date, Pageable pageable);
}

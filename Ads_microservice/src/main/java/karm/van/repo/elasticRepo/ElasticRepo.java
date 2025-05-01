package karm.van.repo.elasticRepo;

import karm.van.model.CardDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

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
                     "minimum_should_match": 1
                 }
             }
            """)
    Page<CardDocument> findByQuery(String query, Pageable pageable);

    @Query("""
       {               
           "bool": {                       
               "should": [                           
                   {                               
                       "multi_match": {                                   
                           "query": "?0",                                   
                           "fields": [                                       
                               "title^4",                                       
                               "text^3"                                   
                           ],                                   
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
                   }                       
               ]                   
           }               
       }
       """)
    Page<CardDocument> findByQueryAndSortByData(String query, String createTime, Pageable pageable);
}

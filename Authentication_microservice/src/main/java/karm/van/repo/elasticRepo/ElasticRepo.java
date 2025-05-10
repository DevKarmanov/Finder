package karm.van.repo.elasticRepo;

import karm.van.model.MyUserDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.repository.query.Param;

public interface ElasticRepo extends ElasticsearchRepository<MyUserDocument,Long> {

    @Query("""
{
  "bool": {
    "should": [
      {
        "match": {
          "name": {
            "query": "?0",
            "boost": 5,
            "fuzziness": "AUTO"
          }
        }
      },
      {
        "match": {
          "firstName": {
            "query": "?1",
            "boost": 4,
            "fuzziness": "AUTO"
          }
        }
      },
      {
        "match": {
          "lastName": {
            "query": "?2",
            "boost": 4,
            "fuzziness": "AUTO"
          }
        }
      },
      {
        "match": {
          "skills": {
            "query": "?3",
            "boost": 3,
            "fuzziness": "AUTO"
          }
        }
      },
      {
        "match": {
          "roleInCommand": {
            "query": "?4",
            "boost": 2,
            "fuzziness": "AUTO"
          }
        }
      },
      {
        "match": {
          "description": {
            "query": "?5",
            "boost": 1.5,
            "fuzziness": "AUTO"
          }
        }
      },
      {
        "match": {
          "country": {
            "query": "?6",
            "boost": 1,
            "fuzziness": "AUTO"
          }
        }
      }
    ],
    "minimum_should_match": 1
  }
}
""")
    Page<MyUserDocument> searchUsers(
            @Param("name") String name,
            @Param("firstname") String firstname,
            @Param("lastname") String lastname,
            @Param("skills") String skills,
            @Param("roleInCommand") String roleInCommand,
            @Param("description") String description,
            @Param("country") String country,
            Pageable pageable);


}

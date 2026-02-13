package com.bsu.cvbuilder.repository;

import com.bsu.cvbuilder.domain.entity.SecureData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SecureDataRepository extends MongoRepository<SecureData, String> {
    @Query("""
            {
              $expr: {
                $gt: [
                  {
                    $size: {
                      $objectToArray: {
                        $ifNull: ["$secureEvents", {}]
                      }
                    }
                  },
                  0
                ]
              }
            }
            """)
    List<SecureData> findWithMultipleSecureEvents();


    Optional<SecureData> findByUserId(String userId);

    void deleteByUserId(String id);
}

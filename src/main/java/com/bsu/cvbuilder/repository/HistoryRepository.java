package com.bsu.cvbuilder.repository;

import com.bsu.cvbuilder.domain.entity.History;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface HistoryRepository extends MongoRepository<History, String>, HistoryEventRepository {

    Optional<History> findByUserId(String userId);

    @Query("""
            {
              $expr: {
                $gt: [
                  {
                    $size: {
                      $objectToArray: {
                        $ifNull: ["$events", {}]
                      }
                    }
                  },
                  1
                ]
              }
            }
            """)
    List<History> findWithMultipleEvents();

    void deleteByUserId(String id);
}

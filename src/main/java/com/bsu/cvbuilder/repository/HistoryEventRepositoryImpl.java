package com.bsu.cvbuilder.repository;


import com.bsu.cvbuilder.domain.dto.history.HistoryEventsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.aggregation.ObjectOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Repository
@RequiredArgsConstructor
public class HistoryEventRepositoryImpl implements HistoryEventRepository {

    public static final String EVENTS_ARRAY = "eventsArray";
    private final MongoTemplate mongoTemplate;

    @Override
    public HistoryEventsDto findByUserId(String id, int page, int size) {
        Aggregation aggregation = Aggregation.newAggregation(
            match(Criteria.where("userId").is(id)),
            project("id")
                .and(ObjectOperators.ObjectToArray.valueOfToArray("events")).as(EVENTS_ARRAY),
            unwind(EVENTS_ARRAY),
            sort(Sort.Direction.ASC, "eventsArray.k"),
            skip((long) page * size),
            limit(size),
            group("_id")
                .push(EVENTS_ARRAY).as(EVENTS_ARRAY),
            project("id")
                .and(ArrayOperators.ArrayToObject.arrayToObject(EVENTS_ARRAY)).as("events")
        );
        return mongoTemplate.aggregate(aggregation, "history", HistoryEventsDto.class)
                            .getUniqueMappedResult();
    }
}
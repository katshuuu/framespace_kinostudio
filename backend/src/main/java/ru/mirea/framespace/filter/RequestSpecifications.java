package ru.mirea.framespace.filter;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import ru.mirea.framespace.domain.ShootingRequest;
import ru.mirea.framespace.domain.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Создаёт параметризованную JPA Specification из разобранного фильтра. */
public final class RequestSpecifications {
    private RequestSpecifications() { }

    public static Specification<ShootingRequest> from(RequestFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            query.distinct(true);

            if (filter.archived() != null) predicates.add(cb.equal(root.get("archived"), filter.archived()));
            if (filter.status() != null) predicates.add(cb.equal(root.get("status"), filter.status()));
            if (filter.urgent() != null) predicates.add(cb.equal(root.get("urgent"), filter.urgent()));
            if (filter.priority() != null) predicates.add(cb.equal(root.get("priorityLevel"), filter.priority()));

            Join<ShootingRequest, Location> locationJoin = root.join("location", JoinType.LEFT);
            if (filter.location() != null) {
                predicates.add(cb.like(cb.lower(locationJoin.get("code")), "%" + escape(filter.location()) + "%", '\\'));
            }
            if (filter.tag() != null) {
                Join<ShootingRequest, String> tagJoin = root.joinSet("tags", JoinType.LEFT);
                predicates.add(cb.equal(cb.lower(tagJoin), filter.tag().toLowerCase(Locale.ROOT)));
            }
            if (filter.freeText() != null && !filter.freeText().isBlank()) {
                String like = "%" + escape(filter.freeText()) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like, '\\'),
                        cb.like(cb.lower(root.get("description")), like, '\\'),
                        cb.like(cb.lower(root.get("clientName")), like, '\\'),
                        cb.like(cb.lower(root.get("clientEmail")), like, '\\'),
                        cb.like(cb.lower(root.get("externalId")), like, '\\'),
                        cb.like(cb.lower(locationJoin.get("code")), like, '\\')));
            }
            filter.budgets().forEach(constraint -> predicates.add(switch (constraint.operator()) {
                case LT -> cb.lessThan(root.get("budget"), constraint.value());
                case LTE -> cb.lessThanOrEqualTo(root.get("budget"), constraint.value());
                case EQ -> cb.equal(root.get("budget"), constraint.value());
                case GTE -> cb.greaterThanOrEqualTo(root.get("budget"), constraint.value());
                case GT -> cb.greaterThan(root.get("budget"), constraint.value());
            }));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String escape(String value) {
        return value.toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}

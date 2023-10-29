package ru.practicum.explore_with_me.util;


import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@NoArgsConstructor
public class QPredicates {
    private List<Predicate> predicates = new ArrayList<>();

    public <T> QPredicates add(Predicate predicate) {
        if (predicate != null) {
            predicates.add(predicate);
        }
        return this;
    }

    public <T> QPredicates add(T object, Function<T, Predicate> function) {
        if (object != null) {
            predicates.add(function.apply(object));
        }
        return this;
    }

    public Predicate buildAnd() {
        return ExpressionUtils.allOf(predicates);
    }

    public Predicate buildOr() {
        return ExpressionUtils.anyOf(predicates);
    }

    public static QPredicates builder() {
        return new QPredicates();
    }
}

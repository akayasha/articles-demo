package com.example.articlesdemo.entity;

import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleEntityTest {

    @Test
    void titleHasUniqueConstraint() throws NoSuchFieldException {
        Field field = Article.class.getDeclaredField("title");
        Column column = field.getAnnotation(Column.class);
        assertThat(column).isNotNull();
        assertThat(column.unique()).isTrue();
    }
}

package com.example.articlesdemo.repository;

import com.example.articlesdemo.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArticleRepository  extends JpaRepository<Article, Long> {

    Optional<Article> findByTitleIgnoreCase(String title);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM articles a WHERE LOWER(a.title) = LOWER(:title)")
    boolean existsByTitle(@Param("title") String title);

}

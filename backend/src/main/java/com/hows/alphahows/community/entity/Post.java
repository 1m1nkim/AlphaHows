package com.hows.alphahows.community.entity;

import com.hows.alphahows.common.BaseTimeEntity;
import com.hows.alphahows.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

/**
 * 게시글 엔티티
 * 커뮤니티(약 후기, 병원 정보, 자유)에 작성된 글 정보를 관리합니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "posts")
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 게시글 고유 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // 작성자

    @Column(nullable = false)
    private String title; // 글 제목

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content; // 글 내용 (긴 글 가능)

    private String category; // 게시판 카테고리 (FREE, HOSPITAL, REVIEW)

    @Column(name = "view_count")
    private int viewCount; // 조회수

    // 게시글에 달린 댓글들 (양방향 매핑, 글 삭제되면 댓글도 자동 삭제)
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private List<Comment> comments = new ArrayList<>();

    @Builder
    public Post(User user, String title, String content, String category) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.category = category;
        this.viewCount = 0;
    }
}

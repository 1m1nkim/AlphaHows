package com.hows.alphahows.community.entity;

import com.hows.alphahows.common.BaseTimeEntity;
import com.hows.alphahows.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.Builder;

/**
 * 댓글 엔티티
 * 게시글에 달리는 댓글 정보를 관리합니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "comments")
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 댓글 고유 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post; // 댓글이 달린 게시글

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // 댓글 작성자

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content; // 댓글 내용

    @Builder
    public Comment(Post post, User user, String content) {
        this.post = post;
        this.user = user;
        this.content = content;
    }
}

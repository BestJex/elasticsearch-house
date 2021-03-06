package com.bubble.house.entity.user;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Role
 *
 * @author wugang
 * date: 2019-11-05 19:15
 **/
@Entity
@Table(name = "role")
public class RoleEntity implements Serializable {
    private static final long serialVersionUID = 5643725097919770739L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id")
    private Long userId;
    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}

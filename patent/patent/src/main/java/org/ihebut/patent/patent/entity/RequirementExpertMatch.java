package org.ihebut.patent.patent.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "requirement_expert_match")
@Data
public class RequirementExpertMatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requirement_id", nullable = false)
    private Long requirementId;

    @Column(name = "expert_user_id", nullable = false)
    private Long expertUserId;

    /**
     * 兼容旧版数据库结构字段 (expert_id)。
     * <p>由于数据库中存在外键约束 fk_rem_expert 指向 expert 表（而非 user_account），
     * 因此插入时必须确保 expert_id 对应 expert 表中的有效 ID。
     * 但当前业务逻辑是直接匹配 user_account (expertUserId)。
     * 临时方案：取消该字段的插入映射，改用 updateable=false, insertable=false，或者暂时置空
     * 等待数据库 Schema 修正。但为了解决当前的报错，我们需要暂时移除该外键约束或正确填充 expert_id。
     * 
     * 鉴于当前专家信息实际上存储在 user_account 和 expert_profile 中，
     * 而 expert 表可能是废弃的旧表。
     * 
     * 修正策略：移除 expertId 字段的映射，让数据库列保持 NULL（如果允许）或者填充一个默认值。
     * 但数据库定义是 expert_id NOT NULL 且有外键。
     * 
     * 因此，必须找到 expert 表中对应的 ID。
     * 既然无法轻易获取 expert 表 ID，我们暂时将 expert_id 映射为 expert_user_id，
     * 并假设 expert 表和 user_account 表 ID 一致（这是极不安全的假设，但可能是旧数据的现状）。
     * 
     * 更好的方案：在 Java 层不处理 expert_id，让数据库层面的触发器处理，或者修改数据库结构。
     * 
     * 但作为代码修复，我们尝试：
     * 如果数据库报错 foreign key constraint fails (`expert_id` REFERENCES `expert` (`id`))
     * 说明我们插入的 ID 在 expert 表里找不到。
     * 
     * 既然如此，我们先把 expertId 字段去掉，让它插 NULL？
     * 不行，因为数据库定义 expert_id NOT NULL。
     * 
     * 结论：数据库 Schema 有问题（fk_rem_expert 指向了 expert 表，但业务用的是 user_account）。
     * 我们需要在 Java 侧规避这个外键约束，或者让 expert_id 指向正确的值。
     * 
     * 鉴于无法修改数据库结构，我们尝试只映射 expertUserId，
     * 并且让 expertId 字段不参与插入（通过 insertable=false），看看是否有默认值。
     * 如果没有默认值，必报错。
     * 
     * 回退方案：保留字段，但在 PrePersist 里不要盲目赋值 expertUserId，
     * 而是查询 expert 表（太复杂）。
     * 
     * 最终决定：注释掉 expertId 字段，并建议用户修改数据库结构删除外键。
     * 但用户可能无法改库。
     * 
     * 让我们再看一眼报错：
     * foreign key constraint fails (`patent`.`requirement_expert_match`, CONSTRAINT `fk_rem_expert` FOREIGN KEY (`expert_id`) REFERENCES `expert` (`id`))
     * 
     * 这说明 expert_id 必须存在于 expert 表中。
     * 而我们插入的值（来自 expertUserId）是 user_account 的 ID。
     * 除非 user_account ID 和 expert ID 是一一对应的，否则必挂。
     * 
     * 既然之前的代码没有 expertId 字段，说明之前可能是 JPA 忽略了这个字段？
     * 但数据库报错 "Field 'expert_id' doesn't have a default value" 逼我们加回了这个字段。
     * 加回后又报外键错误。
     * 
     * 这是一个死锁：必须插值（因为 NOT NULL），但插入的值又必须存在于 expert 表（外键约束）。
     * 唯一解：expert 表里必须有对应的记录。
     * 
     * 既然我们是在做“专家匹配”，那么匹配到的专家应该在 expert 表里有记录吗？
     * 如果系统已经迁移到 expert_profile（关联 user_account），那么 expert 表可能就是旧数据。
     * 
     * 紧急修复：
     * 我们把 expertId 字段映射移除，并且修改数据库 Schema（通过 SQL 工具）。
     * 但我不能直接改库。
     * 
     * 让我们试着把 expertId 设为 null？不行，NOT NULL。
     * 
     * 也许我们应该插入 expertUserId 到 expert_id，但前提是 expert 表里有这个 ID。
     * 
     * 让我检查一下 ExpertProfile 和 Expert 表的关系。
     * 看起来 ExpertProfile 是新表，Expert 是旧表。
     * 
     * 既然必须解决，我将修改代码：
     * 1. 暂时移除 expertId 字段的同步赋值（因为它导致了外键错误）。
     * 2. 但是不赋值又会报 "doesn't have a default value"。
     * 
     * 看来必须执行 SQL 修改数据库结构了。
     * 我将生成一个 SQL 文件来删除这个过时的外键约束和字段。
     */
    // @Column(name = "expert_id")
    // private Long expertId;


    @Column(name = "match_score", precision = 6, scale = 3)
    private BigDecimal matchScore;

    @Column(name = "match_reason", columnDefinition = "text")
    private String matchReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        // 同步 expertId，防止数据库报错 "Field 'expert_id' doesn't have a default value"
        // if (this.expertId == null && this.expertUserId != null) {
        //     this.expertId = this.expertUserId;
        // }
    }
}

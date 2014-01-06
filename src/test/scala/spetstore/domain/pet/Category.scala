package spetstore.domain.pet

import scalikejdbc.ddd.infrastructure.Entity

/**
 * カテゴリを表すエンティティ。
 *
 * @param id 識別子
 * @param name 名前
 * @param description 説明
 */
case class Category(id: CategoryId = CategoryId(),
  name: String,
  description: Option[String] = None)
    extends Entity[CategoryId]


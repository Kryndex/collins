package models

import Model.defaults._
import conversions._
import util.Helpers

import anorm._
import anorm.SqlParser._
import play.api.json._

import java.sql.Timestamp
import java.util.Date

case class AssetLog(
  id: Pk[java.lang.Long],
  asset_id: Id[java.lang.Long],
  created: Timestamp,
  is_error: Boolean,
  is_json: Boolean,
  message: String)
{
  require(message != null && message.length > 0)

  def toMap(): Map[String,String] = Map(
    "ID" -> getId().toString,
    "ASSET_ID" -> getAssetId().toString,
    "CREATED" -> Helpers.dateFormat(created),
    "IS_ERROR" -> is_error.toString,
    "IS_JSON" -> is_json.toString,
    "MESSAGE" -> message
  )

  def getId(): Long = id.get
  def getAssetId(): Long = asset_id.get
  def getAsset(): Asset = Asset.findById(getAssetId()).get
}

case class Page[A](items: Seq[A], page: Int, offset: Long, total: Long) {
  lazy val prev: Option[Int] = Option(page - 1).filter(_ >= 0)
  lazy val next: Option[Int] = Option(page + 1).filter(_ => (offset + items.size) < total)
}

object AssetLog extends Magic[AssetLog](Some("asset_log")) {

  def apply(asset: Asset, message: String, isError: Boolean) = {
    new AssetLog(NotAssigned, Id(asset.getId), new Date().asTimestamp, isError, false, message)
  }
  def apply(asset: Asset, message: JsValue, isError: Boolean) = {
    new AssetLog(NotAssigned, Id(asset.getId), new Date().asTimestamp, isError, true, stringify(message))
  }

  def apply(asset_id: Long, message: String, isError: Boolean) = {
    new AssetLog(NotAssigned, Id(asset_id), new Date().asTimestamp, isError, false, message)
  }
  def apply(asset_id: Long, message: JsValue, isError: Boolean) = {
    new AssetLog(NotAssigned, Id(asset_id), new Date().asTimestamp, isError, true, stringify(message))
  }

  def list(asset: Asset, page: Int = 0, pageSize: Int = 10): Page[AssetLog] = {
    val offset = pageSize * page
    Model.withConnection { implicit con =>
      val rows = AssetLog.find("asset_id={asset_id} limit {pageSize} offset {offset}").on(
        'asset_id -> asset.getId,
        'pageSize -> pageSize,
        'offset -> offset
      ).list()
      val totalRows = SQL("select count(*) from asset_log where asset_id={asset_id}").on(
        'asset_id -> asset.getId
      ).as(scalar[Long])
      Page(rows, page, offset, totalRows)
    }
  }
}

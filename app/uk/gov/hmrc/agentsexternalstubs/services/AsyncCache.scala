package uk.gov.hmrc.agentsexternalstubs.services
import com.github.blemale.scaffeine.Scaffeine

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class AsyncCache[K, V](
  maximumSize: Int,
  expireAfterWrite: Option[Duration] = None,
  expireAfterAccess: Option[Duration] = None) {

  private val cache = {
    val s = Scaffeine().maximumSize(maximumSize)
    val s1 = expireAfterWrite.map(d => s.expireAfterWrite(d)).getOrElse(s)
    val s2 = expireAfterAccess.map(d => s1.expireAfterAccess(d)).getOrElse(s1)
    s2.build[K, V]()
  }

  def get(key: K, load: K => Future[V])(implicit ec: ExecutionContext): Future[V] =
    cache.getIfPresent(key) match {
      case Some(value) => Future.successful(value)
      case None =>
        load(key).map(value => {
          cache.put(key, value)
          value
        })
    }

  def getOption(key: K, load: K => Future[Option[V]])(implicit ec: ExecutionContext): Future[Option[V]] =
    cache.getIfPresent(key) match {
      case Some(value) => Future.successful(Some(value))
      case None =>
        load(key).map {
          _.map(value => {
            cache.put(key, value)
            value
          })
        }
    }

  def put(key: K, value: V): Future[Unit] = {
    cache.put(key, value)
    Future.successful(())
  }

  def put(key: K, value: Option[V]): Future[Unit] = {
    value.foreach(v => cache.put(key, v))
    Future.successful(())
  }

  def invalidate(key: K): Future[Unit] = {
    cache.invalidate(key)
    Future.successful(())
  }
}

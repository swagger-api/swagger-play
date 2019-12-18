package play.modules.swagger

import play.routes.compiler.Route

class RouteWrapper(var router: Map[String, Route]) {
  def get(routeName: String): Option[Route] = router.get(routeName)

  def apply(routeName: String): Route = router(routeName)

  def exists(routeName: String): Boolean = router.contains(routeName)

  def getAll: Map[String, Route] = router
}

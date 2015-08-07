package controllers

import play.mvc.Controller
import play.modules.swagger.ApiHelpInventory
import play.Logger

/**
  * This controller exposes swagger compatiable help apis.<br/>
  * The routing for the two apis supported by this controller is automatically injected by SwaggerPlugin
  *
  * @author ayush
  * @since 10/9/11 4:37 PM
  *
  */
object ApiHelpController extends Controller {
  def catchAll() = {
    Logger.info("ApiHelpController.catchAll got called. This should not happen; SwaggerPlugin.rawInvocation should be intercepting and processing this call")

  }
}
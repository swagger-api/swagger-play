package testdata;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.*;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import java.util.List;

@Api(value = "/apitest/search", description = "Search for settlements", tags = {"Search"})
public class SettlementsSearcherController extends Controller {

  @ApiOperation(value = "Search for settlement", notes = "Search for a settlement with personal number and property id.", httpMethod = "GET", nickname = "getsettlement", produces = "application/json", response = Settlement.class, responseContainer = "List")
  @ApiResponses({
      @ApiResponse(code = Http.Status.BAD_REQUEST, message = "Bad Request"),
      @ApiResponse(code = Http.Status.UNAUTHORIZED, message = "Unauthorized"),
      @ApiResponse(code = Http.Status.INTERNAL_SERVER_ERROR, message = "Server error")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(value = "Token for logged in user", name = "Authorization", required = false, dataType = "string", paramType = "header"),
  })
  public Result search(
      @ApiParam(value = "A personal number of one of the sellers.", example = "0101201112345") String personalNumber,
      @ApiParam(value = "The cadastre or share id.", example = "1201-5-1-0-0", required = true) String propertyId) {
    return ok();
  }
}

@JsonInclude(JsonInclude.Include.NON_EMPTY)
class Settlement {

  @ApiModelProperty(required = true)
  public String settlementId;

  @ApiModelProperty(required = true)
  public List<String> sellers;

  @ApiModelProperty
  public List<String> buyers;

  @ApiModelProperty
  public Integer purchaseAmount;

}


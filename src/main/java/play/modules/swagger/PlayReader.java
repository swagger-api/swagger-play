package play.modules.swagger;

import com.fasterxml.jackson.databind.JavaType;
import io.swagger.annotations.*;
import io.swagger.annotations.Info;
import io.swagger.converter.ModelConverters;
import io.swagger.models.Contact;
import io.swagger.models.ExternalDocs;
import io.swagger.models.*;
import io.swagger.models.Tag;
import io.swagger.models.auth.In;
import io.swagger.models.parameters.*;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.*;
import io.swagger.util.*;
import org.apache.commons.lang3.StringUtils;
import play.Logger;
import play.modules.swagger.util.CrossUtil;
import play.routes.compiler.*;
import scala.Option;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayReader {

    private static final String SUCCESSFUL_OPERATION = "successful operation";

    private Swagger swagger;
    private PlaySwaggerConfig config;
    private RouteWrapper routes;

    public Swagger getSwagger() {
        return swagger;
    }

    public PlayReader(PlaySwaggerConfig config, RouteWrapper routes, Swagger swagger) {
        this.routes = routes;
        this.config = config;
        this.swagger = swagger == null ? new Swagger() : swagger;
    }

    public Swagger read(Set<Class<?>> classes) {

        // process SwaggerDefinitions first - so we get tags in desired order
        for (Class<?> cls : classes) {
            SwaggerDefinition swaggerDefinition = cls.getAnnotation(SwaggerDefinition.class);
            if (swaggerDefinition != null) {
                readSwaggerConfig(cls, swaggerDefinition);
            }
        }

        for (Class<?> cls : classes) {
            read(cls);
        }
        return swagger;
    }

    public Swagger read(Class<?> cls) {
        return read(cls, false);
    }

    private Swagger read(Class<?> cls, boolean readHidden) {
        Api api = cls.getAnnotation(Api.class);

        Map<String, Tag> tags = new HashMap<>();
        List<SecurityRequirement> securities = new ArrayList<>();
        String[] consumes = new String[0];
        String[] produces = new String[0];
        final Set<Scheme> globalSchemes = EnumSet.noneOf(Scheme.class);

        final boolean readable = (api != null && readHidden) || (api != null && !api.hidden());

        // TODO possibly allow parsing also without @Api annotation
        if (readable) {
            // the value will be used as a tag for 2.0 UNLESS a Tags annotation is present
            Set<String> tagStrings = extractTags(api);
            for (String tagString : tagStrings) {
                Tag tag = new Tag().name(tagString);
                tags.put(tagString, tag);
            }
            for (String tagName : tags.keySet()) {
                getSwagger().tag(tags.get(tagName));
            }

            if (!api.produces().isEmpty()) {
                produces = toArray(api.produces());
            }
            if (!api.consumes().isEmpty()) {
                consumes = toArray(api.consumes());
            }
            globalSchemes.addAll(parseSchemes(api.protocols()));
            Authorization[] authorizations = api.authorizations();

            for (Authorization auth : authorizations) {
                if (auth.value() != null && !"".equals(auth.value())) {
                    SecurityRequirement security = new SecurityRequirement();
                    security.setName(auth.value());
                    AuthorizationScope[] scopes = auth.scopes();
                    for (AuthorizationScope scope : scopes) {
                        if (scope.scope() != null && !"".equals(scope.scope())) {
                            security.addScope(scope.scope());
                        }
                    }
                    securities.add(security);
                }
            }

            // parse the method
            Method methods[] = cls.getMethods();
            for (Method method : methods) {
                if (ReflectionUtils.isOverriddenMethod(method, cls)) {
                    continue;
                }
                // complete name as stored in route
                String fullMethodName = getFullMethodName(cls, method);

                if (!routes.exists(fullMethodName)) {
                    continue;
                }
                Route route = routes.apply(fullMethodName);

                String operationPath = getPathFromRoute(route.path(), config.basePath());

                if (operationPath != null) {
                    final ApiOperation apiOperation = ReflectionUtils.getAnnotation(method, ApiOperation.class);

                    String httpMethod = extractOperationMethod(apiOperation, method, route);
                    Operation operation = null;
                    if (apiOperation != null || httpMethod != null) {
                        operation = parseMethod(cls, method, route);
                    }

                    if (operation == null) {
                        continue;
                    }

                    if (apiOperation != null) {
                        for (Scheme scheme : parseSchemes(apiOperation.protocols())) {
                            operation.scheme(scheme);
                        }
                    }

                    if (operation.getSchemes() == null || operation.getSchemes().isEmpty()) {
                        for (Scheme scheme : globalSchemes) {
                            operation.scheme(scheme);
                        }
                    }
                    // can't continue without a valid http method
                    if (httpMethod != null) {
                        if (apiOperation != null) {
                            for (String tag : apiOperation.tags()) {
                                if (!"".equals(tag)) {
                                    operation.tag(tag);
                                    getSwagger().tag(new Tag().name(tag));
                                }
                            }

                            operation.getVendorExtensions()
                                    .putAll(BaseReaderUtils.parseExtensions(apiOperation.extensions()));
                        }
                        if (operation.getConsumes() == null) {
                            for (String mediaType : consumes) {
                                operation.consumes(mediaType);
                            }
                        }
                        if (operation.getProduces() == null) {
                            for (String mediaType : produces) {
                                operation.produces(mediaType);
                            }
                        }

                        if (operation.getTags() == null) {
                            for (String tagString : tags.keySet()) {
                                operation.tag(tagString);
                            }
                        }
                        // Only add global @Api securities if operation doesn't already have more
                        // specific securities
                        if (operation.getSecurity() == null) {
                            for (SecurityRequirement security : securities) {
                                operation.security(security);
                            }
                        }
                        Path path = getSwagger().getPath(operationPath);
                        if (path == null) {
                            path = new Path();
                            getSwagger().path(operationPath, path);
                        }
                        path.set(httpMethod, operation);
                        try {
                            readImplicitParameters(method, operation, cls);
                        } catch (Exception e) {
                            throw e;
                        }
                    }
                }
            }
        }

        return getSwagger();
    }

    String getPathFromRoute(PathPattern pathPattern, String basePath) {

        StringBuilder sb = new StringBuilder();
        scala.collection.Iterator iter = pathPattern.parts().iterator();
        while (iter.hasNext()) {
            PathPart part = (PathPart) iter.next();
            if (part instanceof StaticPart) {
                sb.append(((StaticPart) part).value());
            } else if (part instanceof DynamicPart) {
                sb.append("{");
                sb.append(((DynamicPart) part).name());
                sb.append("}");
            } else {
                try {
                    sb.append(((StaticPart) part).value());
                } catch (ClassCastException e) {
                    Logger.of("swagger")
                            .warn(String.format("ClassCastException parsing path from route: %s", e.getMessage()));
                }
            }
        }
        StringBuilder basePathFilter = new StringBuilder(basePath);
        if (basePath.startsWith("/"))
            basePathFilter.deleteCharAt(0);
        if (!basePath.endsWith("/"))
            basePathFilter.append("/");
        String basePathString = basePathFilter.toString();

        String pathPatternString = sb.toString();
        StringBuilder operationPath = new StringBuilder();
        if ((pathPatternString.startsWith("/") && pathPatternString.startsWith(basePathString, 1))
                || (pathPatternString.startsWith(basePathString)))
            operationPath.append(pathPatternString.replaceFirst(basePathString, ""));
        else
            operationPath.append(pathPatternString);
        if (!operationPath.toString().startsWith("/"))
            operationPath.insert(0, "/");
        return operationPath.toString();
    }

    protected void readSwaggerConfig(Class<?> cls, SwaggerDefinition config) {

        if (!config.basePath().isEmpty()) {
            swagger.setBasePath(config.basePath());
        }

        if (!config.host().isEmpty()) {
            swagger.setHost(config.host());
        }

        readInfoConfig(config);

        for (String consume : config.consumes()) {
            if (StringUtils.isNotEmpty(consume)) {
                swagger.addConsumes(consume);
            }
        }

        for (String produce : config.produces()) {
            if (StringUtils.isNotEmpty(produce)) {
                swagger.addProduces(produce);
            }
        }

        if (!config.externalDocs().value().isEmpty()) {
            ExternalDocs externalDocs = swagger.getExternalDocs();
            if (externalDocs == null) {
                externalDocs = new ExternalDocs();
                swagger.setExternalDocs(externalDocs);
            }

            externalDocs.setDescription(config.externalDocs().value());

            if (!config.externalDocs().url().isEmpty()) {
                externalDocs.setUrl(config.externalDocs().url());
            }
        }

        for (io.swagger.annotations.Tag tagConfig : config.tags()) {
            if (!tagConfig.name().isEmpty()) {
                Tag tag = new Tag();
                tag.setName(tagConfig.name());
                tag.setDescription(tagConfig.description());

                if (!tagConfig.externalDocs().value().isEmpty()) {
                    tag.setExternalDocs(
                            new ExternalDocs(tagConfig.externalDocs().value(), tagConfig.externalDocs().url()));
                }

                tag.getVendorExtensions().putAll(BaseReaderUtils.parseExtensions(tagConfig.extensions()));

                swagger.addTag(tag);
            }
        }

        for (ApiKeyAuthDefinition ann : config.securityDefinition().apiKeyAuthDefinitions()) {
            io.swagger.models.auth.ApiKeyAuthDefinition defn = new io.swagger.models.auth.ApiKeyAuthDefinition();
            defn.setName(ann.name());
            defn.setIn(In.forValue(ann.in().toValue()));
            defn.setDescription(ann.description());
            swagger.addSecurityDefinition(ann.key(), defn);
        }

        for (BasicAuthDefinition ann : config.securityDefinition().basicAuthDefinitions()) {
            io.swagger.models.auth.BasicAuthDefinition defn = new io.swagger.models.auth.BasicAuthDefinition();
            defn.setDescription(ann.description());
            swagger.addSecurityDefinition(ann.key(), defn);
        }

        for (OAuth2Definition ann : config.securityDefinition().oAuth2Definitions()) {
            io.swagger.models.auth.OAuth2Definition defn = new io.swagger.models.auth.OAuth2Definition();
            defn.setTokenUrl(ann.tokenUrl());
            defn.setAuthorizationUrl(ann.authorizationUrl());
            defn.setFlow(ann.flow().name().toLowerCase());
            for (Scope scope : ann.scopes()) {
                defn.addScope(scope.name(), scope.description());
            }
            swagger.addSecurityDefinition(ann.key(), defn);
        }

        for (SwaggerDefinition.Scheme scheme : config.schemes()) {
            if (scheme != SwaggerDefinition.Scheme.DEFAULT) {
                swagger.addScheme(Scheme.forValue(scheme.name()));
            }
        }
    }

    protected void readInfoConfig(SwaggerDefinition config) {
        Info infoConfig = config.info();
        io.swagger.models.Info info = swagger.getInfo();
        if (info == null) {
            info = new io.swagger.models.Info();
            swagger.setInfo(info);
        }

        if (!infoConfig.description().isEmpty()) {
            info.setDescription(infoConfig.description());
        }

        if (!infoConfig.termsOfService().isEmpty()) {
            info.setTermsOfService(infoConfig.termsOfService());
        }

        if (!infoConfig.title().isEmpty()) {
            info.setTitle(infoConfig.title());
        }

        if (!infoConfig.version().isEmpty()) {
            info.setVersion(infoConfig.version());
        }

        if (!infoConfig.contact().name().isEmpty()) {
            Contact contact = info.getContact();
            if (contact == null) {
                contact = new Contact();
                info.setContact(contact);
            }

            contact.setName(infoConfig.contact().name());
            if (!infoConfig.contact().email().isEmpty()) {
                contact.setEmail(infoConfig.contact().email());
            }

            if (!infoConfig.contact().url().isEmpty()) {
                contact.setUrl(infoConfig.contact().url());
            }
        }

        if (!infoConfig.license().name().isEmpty()) {
            io.swagger.models.License license = info.getLicense();
            if (license == null) {
                license = new io.swagger.models.License();
                info.setLicense(license);
            }

            license.setName(infoConfig.license().name());
            if (!infoConfig.license().url().isEmpty()) {
                license.setUrl(infoConfig.license().url());
            }
        }

        info.getVendorExtensions().putAll(BaseReaderUtils.parseExtensions(infoConfig.extensions()));
    }

    private void readImplicitParameters(Method method, Operation operation, Class<?> cls) {
        ApiImplicitParams implicitParams = method.getAnnotation(ApiImplicitParams.class);
        if (implicitParams != null && implicitParams.value().length > 0) {
            for (ApiImplicitParam param : implicitParams.value()) {
                Parameter p = readImplicitParam(param, cls);
                if (p != null) {
                    operation.addParameter(p);
                }
            }
        }
    }

    protected io.swagger.models.parameters.Parameter readImplicitParam(ApiImplicitParam param, Class<?> cls) {
        final Parameter p;
        if (param.paramType().equalsIgnoreCase("path")) {
            p = new PathParameter();
        } else if (param.paramType().equalsIgnoreCase("query")) {
            p = new QueryParameter();
        } else if (param.paramType().equalsIgnoreCase("form") || param.paramType().equalsIgnoreCase("formData")) {
            p = new FormParameter();
        } else if (param.paramType().equalsIgnoreCase("body")) {
            p = null;
        } else if (param.paramType().equalsIgnoreCase("header")) {
            p = new HeaderParameter();
        } else {
            Logger.of("swagger").warn("Unkown implicit parameter type: [" + param.paramType() + "]");
            return null;
        }
        Type type = null;
        // Swagger ReflectionUtils can't handle file or array datatype
        if (!"".equalsIgnoreCase(param.dataType()) && !"file".equalsIgnoreCase(param.dataType())
                && !"array".equalsIgnoreCase(param.dataType())) {
            type = typeFromString(param.dataType(), cls);
        } else if (param.dataTypeClass() != null && !isVoid(param.dataTypeClass())) {
            type = param.dataTypeClass();
        }

        Parameter result = ParameterProcessor.applyAnnotations(getSwagger(), p, type == null ? String.class : type,
                Collections.singletonList(param));

        if (result instanceof AbstractSerializableParameter && type != null) {
            Property schema = createProperty(type);
            ((AbstractSerializableParameter) p).setProperty(schema);
        }

        return result;

    }

    private static Type typeFromString(String type, Class<?> cls) {
        final PrimitiveType primitive = PrimitiveType.fromName(type);
        if (primitive != null) {
            return primitive.getKeyClass();
        }
        try {
            Type routeType = getOptionTypeFromString(type, cls);

            if (routeType != null)
                return routeType;

            return Thread.currentThread().getContextClassLoader().loadClass(type);
        } catch (Exception e) {
            Logger.of("swagger").error(String.format("Failed to resolve '%s' into class", type), e);
        }
        return null;
    }

    private Operation parseMethod(Class<?> cls, Method method, Route route) {
        Operation operation = new Operation();

        ApiOperation apiOperation = ReflectionUtils.getAnnotation(method, ApiOperation.class);
        ApiResponses responseAnnotation = ReflectionUtils.getAnnotation(method, ApiResponses.class);

        String operationId = method.getName();
        operation.operationId(operationId);
        String responseContainer = null;

        Type responseType = null;
        Map<String, Property> defaultResponseHeaders = new HashMap<>();

        if (apiOperation != null) {
            if (apiOperation.hidden()) {
                return null;
            }
            if (!"".equals(apiOperation.nickname())) {
                operationId = apiOperation.nickname();
            }

            defaultResponseHeaders = parseResponseHeaders(apiOperation.responseHeaders());

            operation.summary(apiOperation.value()).description(apiOperation.notes());

            if (apiOperation.response() != null && !isVoid(apiOperation.response())) {
                responseType = apiOperation.response();
            }
            if (!"".equals(apiOperation.responseContainer())) {
                responseContainer = apiOperation.responseContainer();
            }
            if (apiOperation.authorizations() != null) {
                List<SecurityRequirement> securities = new ArrayList<>();
                for (Authorization auth : apiOperation.authorizations()) {
                    if (auth.value() != null && !"".equals(auth.value())) {
                        SecurityRequirement security = new SecurityRequirement();
                        security.setName(auth.value());
                        AuthorizationScope[] scopes = auth.scopes();
                        for (AuthorizationScope scope : scopes) {
                            if (scope.scope() != null && !"".equals(scope.scope())) {
                                security.addScope(scope.scope());
                            }
                        }
                        securities.add(security);
                    }
                }
                if (securities.size() > 0) {
                    securities.forEach(operation::security);
                }
            }
            if (apiOperation.consumes() != null && !apiOperation.consumes().isEmpty()) {
                operation.consumes(Arrays.asList(toArray(apiOperation.consumes())));
            }
            if (apiOperation.produces() != null && !apiOperation.produces().isEmpty()) {
                operation.produces(Arrays.asList(toArray(apiOperation.produces())));
            }
        }

        if (apiOperation != null && StringUtils.isNotEmpty(apiOperation.responseReference())) {
            Response response = new Response().description(SUCCESSFUL_OPERATION);
            response.schema(new RefProperty(apiOperation.responseReference()));
            operation.addResponse(String.valueOf(apiOperation.code()), response);
        } else if (responseType == null) {
            // pick out response from method declaration
            responseType = method.getGenericReturnType();
        }
        if (isValidResponse(responseType)) {
            final Property property = ModelConverters.getInstance().readAsProperty(responseType);
            if (property != null) {
                final Property responseProperty = ContainerWrapper.wrapContainer(responseContainer, property);
                final int responseCode = apiOperation == null ? 200 : apiOperation.code();
                operation.response(responseCode, new Response().description(SUCCESSFUL_OPERATION)
                        .schema(responseProperty).headers(defaultResponseHeaders));
                appendModels(responseType);
            }
        }

        operation.operationId(operationId);

        if (responseAnnotation != null) {
            for (ApiResponse apiResponse : responseAnnotation.value()) {
                Map<String, Property> responseHeaders = parseResponseHeaders(apiResponse.responseHeaders());

                Response response = new Response().description(apiResponse.message()).headers(responseHeaders);

                if (apiResponse.code() == 0) {
                    operation.defaultResponse(response);
                } else {
                    operation.response(apiResponse.code(), response);
                }

                if (StringUtils.isNotEmpty(apiResponse.reference())) {
                    response.schema(new RefProperty(apiResponse.reference()));
                } else if (!isVoid(apiResponse.response())) {
                    responseType = apiResponse.response();
                    final Property property = ModelConverters.getInstance().readAsProperty(responseType);
                    if (property != null) {
                        response.schema(ContainerWrapper.wrapContainer(apiResponse.responseContainer(), property));
                        appendModels(responseType);
                    }
                }
            }
        }
        if (ReflectionUtils.getAnnotation(method, Deprecated.class) != null) {
            operation.setDeprecated(true);
        }

        List<Parameter> parameters = getParameters(cls, method, route);

        parameters.forEach(operation::parameter);

        if (operation.getResponses() == null) {
            Response response = new Response().description(SUCCESSFUL_OPERATION);
            operation.defaultResponse(response);
        }
        return operation;
    }

    final static class OptionTypeResolver {
        private Option<Integer> optionTypeInt;
        private Option<Long> optionTypeLong;
        private Option<Byte> optionTypeByte;
        private Option<Boolean> optionTypeBoolean;
        private Option<Character> optionTypeChar;
        private Option<Float> optionTypeFloat;
        private Option<Double> optionTypeDouble;
        private Option<Short> optionTypeShort;

        static Type resolveOptionType(String innerType, Class<?> cls) {
            try {
                return Json.mapper().getTypeFactory().constructType(
                        OptionTypeResolver.class.getDeclaredField("optionType" + innerType).getGenericType(), cls);
            } catch (NoSuchFieldException e) {
                return null;
            }
        }
    }

    private static Type getOptionTypeFromString(String simpleTypeName, Class<?> cls) {

        if (simpleTypeName == null)
            return null;
        String regex = "(Option|scala\\.Option)\\s*\\[\\s*(Int|Long|Float|Double|Byte|Short|Char|Boolean)\\s*\\]\\s*$";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(simpleTypeName);
        if (matcher.find()) {
            String enhancedType = matcher.group(2);
            return OptionTypeResolver.resolveOptionType(enhancedType, cls);
        } else {
            return null;
        }
    }

    private Type getParamType(Class<?> cls, Method method, String simpleTypeName, int position) {

        try {
            Type type = getOptionTypeFromString(simpleTypeName, cls);
            if (type != null)
                return type;

            Type[] genericParameterTypes = method.getGenericParameterTypes();
            return Json.mapper().getTypeFactory().constructType(genericParameterTypes[position], cls);
        } catch (Exception e) {
            Logger.of("swagger")
                    .error(String.format("Exception getting parameter type for method %s, param %s at position %d",
                            method, simpleTypeName, position), e);
            return null;
        }

    }

    private List<Annotation> getParamAnnotations(Class<?> cls, Type[] genericParameterTypes,
            Annotation[][] paramAnnotations, String simpleTypeName, int fieldPosition) {
        try {
            return Arrays.asList(paramAnnotations[fieldPosition]);
        } catch (Exception e) {
            Logger.of("swagger").error(String.format("Exception getting parameter type for %s at position %d",
                    simpleTypeName, fieldPosition), e);
            return null;
        }
    }

    private List<Annotation> getParamAnnotations(Class<?> cls, Method method, String simpleTypeName,
            int fieldPosition) {
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        Annotation[][] paramAnnotations = method.getParameterAnnotations();
        List<Annotation> annotations = getParamAnnotations(cls, genericParameterTypes, paramAnnotations, simpleTypeName,
                fieldPosition);
        if (annotations != null) {
            return annotations;
        }

        // Fallback to type
        for (int i = 0; i < genericParameterTypes.length; i++) {
            annotations = getParamAnnotations(cls, genericParameterTypes, paramAnnotations, simpleTypeName, i);
            if (annotations != null) {
                return annotations;
            }
        }
        return null;
    }

    private List<Parameter> getParameters(Class<?> cls, Method method, Route route) {
        // TODO now consider only parameters defined in route, excluding body parameters
        // understand how to possibly infer body/form params e.g. from @BodyParser or
        // other annotation

        List<Parameter> parameters = new ArrayList<>();
        if (!route.call().parameters().isDefined()) {
            return parameters;
        }
        scala.collection.Iterator<play.routes.compiler.Parameter> iter = route.call().parameters().get().iterator();

        int fieldPosition = 0;
        while (iter.hasNext()) {
            play.routes.compiler.Parameter p = iter.next();
            if (!p.fixed().isEmpty())
                continue;
            Parameter parameter;
            String def = CrossUtil.getParameterDefaultField(p);
            if (def.startsWith("\"") && def.endsWith("\"")) {
                def = def.substring(1, def.length() - 1);
            }
            Type type = getParamType(cls, method, p.typeName(), fieldPosition);
            Property schema = createProperty(type);
            if (route.path().has(p.name())) {
                // it's a path param
                parameter = new PathParameter();
                ((PathParameter) parameter).setDefaultValue(def);
                if (schema != null)
                    ((PathParameter) parameter).setProperty(schema);
            } else {
                // it's a query string param
                parameter = new QueryParameter();
                ((QueryParameter) parameter).setDefaultValue(def);
                if (schema != null)
                    ((QueryParameter) parameter).setProperty(schema);
            }
            parameter.setName(p.name());
            List<Annotation> annotations = getParamAnnotations(cls, method, p.typeName(), fieldPosition);
            ParameterProcessor.applyAnnotations(getSwagger(), parameter, type, annotations);
            parameters.add(parameter);
            fieldPosition++;
        }
        return parameters;
    }

    private static Set<Scheme> parseSchemes(String schemes) {
        final Set<Scheme> result = EnumSet.noneOf(Scheme.class);
        for (String item : StringUtils.trimToEmpty(schemes).split(",")) {
            final Scheme scheme = Scheme.forValue(StringUtils.trimToNull(item));
            if (scheme != null) {
                result.add(scheme);
            }
        }
        return result;
    }

    private static boolean isVoid(Type type) {
        final Class<?> cls = Json.mapper().getTypeFactory().constructType(type).getRawClass();
        return Void.class.isAssignableFrom(cls) || Void.TYPE.isAssignableFrom(cls);
    }

    private static boolean isValidResponse(Type type) {
        if (type == null) {
            return false;
        }
        final JavaType javaType = Json.mapper().getTypeFactory().constructType(type);
        if (isVoid(javaType)) {
            return false;
        }
        final Class<?> cls = javaType.getRawClass();
        return !isResourceClass(cls);
    }

    private static boolean isResourceClass(Class<?> cls) {
        return cls.getAnnotation(Api.class) != null;
    }

    protected Set<String> extractTags(Api api) {
        Set<String> output = new LinkedHashSet<>();

        boolean hasExplicitTags = false;
        for (String tag : api.tags()) {
            if (!"".equals(tag)) {
                hasExplicitTags = true;
                output.add(tag);
            }
        }
        if (!hasExplicitTags) {
            // derive tag from api path + description
            String tagString = api.value().replace("/", "");
            if (!"".equals(tagString)) {
                output.add(tagString);
            }
        }
        return output;
    }

    private Property createProperty(Type type) {
        return enforcePrimitive(ModelConverters.getInstance().readAsProperty(type), 0);
    }

    private Property enforcePrimitive(Property in, int level) {
        if (in instanceof RefProperty) {
            return new StringProperty();
        }
        if (in instanceof ArrayProperty) {
            if (level == 0) {
                final ArrayProperty array = (ArrayProperty) in;
                array.setItems(enforcePrimitive(array.getItems(), level + 1));
            } else {
                return new StringProperty();
            }
        }
        return in;
    }

    private void appendModels(Type type) {
        final Map<String, Model> models = ModelConverters.getInstance().readAll(type);
        for (Map.Entry<String, Model> entry : models.entrySet()) {
            getSwagger().model(entry.getKey(), entry.getValue());
        }
    }

    private Map<String, Property> parseResponseHeaders(ResponseHeader[] headers) {
        Map<String, Property> responseHeaders = null;
        if (headers != null && headers.length > 0) {
            for (ResponseHeader header : headers) {
                String name = header.name();
                if (!"".equals(name)) {
                    if (responseHeaders == null) {
                        responseHeaders = new HashMap<>();
                    }
                    String description = header.description();
                    Class<?> cls = header.response();

                    if (!isVoid(cls)) {
                        final Property property = ModelConverters.getInstance().readAsProperty(cls);
                        if (property != null) {
                            Property responseProperty = ContainerWrapper.wrapContainer(header.responseContainer(),
                                    property, ContainerWrapper.ARRAY, ContainerWrapper.LIST, ContainerWrapper.SET);
                            responseProperty.setDescription(description);
                            responseHeaders.put(name, responseProperty);
                            appendModels(cls);
                        }
                    }
                }
            }
        }
        return responseHeaders;
    }

    public String getFullMethodName(Class clazz, Method method) {

        if (!clazz.getCanonicalName().contains("$")) {
            return clazz.getCanonicalName() + "$." + method.getName();
        } else {
            return clazz.getCanonicalName() + "." + method.getName();
        }
    }

    public String extractOperationMethod(ApiOperation apiOperation, Method method, Route route) {
        String httpMethod = null;
        if (route != null) {
            try {
                httpMethod = route.verb().toString().toLowerCase();
            } catch (Exception e) {
                Logger.of("swagger").error("http method not found for method: " + method.getName(), e);
            }
        }
        if (httpMethod == null) {
            if (!StringUtils.isEmpty(apiOperation.httpMethod())) {
                httpMethod = apiOperation.httpMethod();
            }
        }
        return httpMethod;
    }

    private String[] toArray(String csString) {
        if (StringUtils.isEmpty(csString))
            return new String[] { csString };
        int i = 0;
        String[] result = csString.split(",");
        for (String c : result) {
            result[i] = c.trim();
            i++;
        }
        return result;
    }

    enum ContainerWrapper {
        LIST("list") {
            @Override
            protected Property doWrap(Property property) {
                return new ArrayProperty(property);
            }
        },
        ARRAY("array") {
            @Override
            protected Property doWrap(Property property) {
                return new ArrayProperty(property);
            }
        },
        MAP("map") {
            @Override
            protected Property doWrap(Property property) {
                return new MapProperty(property);
            }
        },
        SET("set") {
            @Override
            protected Property doWrap(Property property) {
                ArrayProperty arrayProperty = new ArrayProperty(property);
                arrayProperty.setUniqueItems(true);
                return arrayProperty;
            }
        };

        private final String container;

        ContainerWrapper(String container) {
            this.container = container;
        }

        public static Property wrapContainer(String container, Property property, ContainerWrapper... allowed) {
            final Set<ContainerWrapper> tmp = allowed.length > 0 ? EnumSet.copyOf(Arrays.asList(allowed))
                    : EnumSet.allOf(ContainerWrapper.class);
            for (ContainerWrapper wrapper : tmp) {
                final Property prop = wrapper.wrap(container, property);
                if (prop != null) {
                    return prop;
                }
            }
            return property;
        }

        public Property wrap(String container, Property property) {
            if (this.container.equalsIgnoreCase(container)) {
                return doWrap(property);
            }
            return null;
        }

        protected abstract Property doWrap(Property property);
    }
}

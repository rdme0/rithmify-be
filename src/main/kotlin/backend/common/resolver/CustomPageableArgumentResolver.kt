package backend.common.resolver

import backend.common.annotation.ResolvePageable
import backend.common.converter.CustomPageRequestToPageableConverter
import backend.common.dto.request.CustomPageRequest
import org.springframework.core.MethodParameter
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class CustomPageableArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(ResolvePageable::class.java) &&
                parameter.parameterType == Pageable::class.java
    }

    override fun resolveArgument(
            parameter: MethodParameter,
            mavContainer: ModelAndViewContainer?,
            webRequest: NativeWebRequest,
            binderFactory: WebDataBinderFactory?
    ): Any {
        val annotation = parameter.getParameterAnnotation(ResolvePageable::class.java)!!
        val allowedFields = annotation.allowed.toList()

        val sort = webRequest.getParameter("sort")
        val direction = webRequest.getParameter("direction")
        val pageStr = webRequest.getParameter("page")
        val sizeStr = webRequest.getParameter("size")

        val request =
                CustomPageRequest(
                        sort = sort,
                        direction = direction,
                        page = pageStr?.toIntOrNull(),
                        size = sizeStr?.toIntOrNull()
                )

        return CustomPageRequestToPageableConverter(allowedFields).convert(request)
    }
}

package backend.common.annotation

import backend.common.constant.TrackSortField

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class ResolvePageable(val allowed: Array<TrackSortField>)

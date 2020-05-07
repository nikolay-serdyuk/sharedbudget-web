package sharedbudget

import org.springframework.data.jpa.domain.Specification
import javax.persistence.criteria.*
import kotlin.reflect.KProperty1

// Helper to allow joining to Properties
fun <Z, T, R> From<Z, T>.join(prop: KProperty1<T, R?>): Join<T, R> = this.join<T, R>(prop.name)

// Helper to enable get by Property
fun <R> Path<*>.get(prop: KProperty1<*, R?>): Path<R> = this.get<R>(prop.name)

// Version of Specification.where that makes the CriteriaBuilder implicit
fun <T> where(makePredicate: CriteriaBuilder.(Root<T>) -> Predicate): Specification<T> =
    Specification { root, _, criteriaBuilder -> criteriaBuilder.makePredicate(root) }

// helper function for defining Specification that take a Path to a property and send it to a CriteriaBuilder
private fun <T, R> KProperty1<T, R>.spec(makePredicate: CriteriaBuilder.(path: Path<R>) -> Predicate): Specification<T> =
    let { property -> where { root -> makePredicate(root.get(property)) } }

// Equality
fun <T, R> KProperty1<T, R>.equal(x: R): Specification<T> = spec { equal(it, x) }

// Ignores empty collection otherwise an empty 'in' predicate will be generated which will never match any results
fun <T, R : Any> KProperty1<T, R?>.`in`(values: Collection<R>): Specification<T> = spec { path ->
    `in`(path).apply { values.forEach { this.value(it) } }
}

// Collections
fun <T, R : Collection<*>> KProperty1<T, R?>.isNotEmpty() = spec { isNotEmpty(it) }

// And
infix fun <T> Specification<T>.and(other: Specification<T>): Specification<T> = this.and(other)

// Not
operator fun <T> Specification<T>.not(): Specification<T> = Specification.not(this)
package sharedbudget.entities

/**
 * This interface is used to overcome one well-known Hibernate problem. Assume we have a
 * set of elements that represents some records in a table. If we modify our set and save
 * it then we may fail because of constraint violation. This is because Hibernate executes
 * operations in the following order:
 * 1 - insertions
 * 2 - updates
 * 3 - deletions
 * So, when we save our set, Hibernate will try to insert elements that already exist in
 * the table and that is why unique constraint violation occurs. One of the solutions is to
 * find all existing elements and update them field by field. In that case, Hibernate will
 * use update operation for existing elements instead of insert.
 */
interface UpdatableEntity<T> {
    fun updateFrom(other: T): T
}

fun <T: UpdatableEntity<in T>> MutableSet<T>.updateAllFrom(source: Iterable<T>) {
    addAll(source)
    val destinationMap = associateBy { it }
    source.forEach {
        val oldValue = destinationMap.getValue(it)
        oldValue.updateFrom(it)
    }
}

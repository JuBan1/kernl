package data.repo

import io.github.mattshoe.shoebox.data.repo.BaseSingleCacheLiveRepository
import kotlinx.coroutines.CoroutineDispatcher

class TestSingleCacheLiveRepository(
    dispatcher: CoroutineDispatcher
): BaseSingleCacheLiveRepository<Int, String>(dispatcher) {
    override val dataType = String::class
    var operation: suspend (Int) -> String = { it.toString() }

    override suspend fun fetchData(params: Int): String {
        return operation(params)
    }

}
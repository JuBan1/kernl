package singlememorycache.impl

import org.mattshoe.shoebox.kernl.runtime.repo.singlecache.SingleCacheLiveRepository
import kernl.org.mattshoe.shoebox.kernl.singlememorycache.SingleParamPrimitiveReturn
import singlememorycache.SingleMemoryCacheScenariosTest

class SingleParamPrimitiveReturnTest: SingleMemoryCacheScenariosTest<SingleParamPrimitiveReturn.Params, Int>() {
    override fun repository(): SingleCacheLiveRepository<SingleParamPrimitiveReturn.Params, Int> {
        return SingleParamPrimitiveReturn.Factory { params ->
            params.toInt()
        }
    }

    override val testData = mapOf(
        SingleParamPrimitiveReturn.Params("42") to 42,
        SingleParamPrimitiveReturn.Params("96") to 96,
        SingleParamPrimitiveReturn.Params("1") to 1
    )
}

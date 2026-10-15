package id.sela.model
import org.junit.Assert.*
import org.junit.Test
class ServicePresentationTest {
    private fun route(code: String) = Route(code, code, code, ServiceType.BUS)
    @Test fun jakCodesAreBlueCategoryWithoutChangingSource() { val r = route("JAK.01"); assertEquals(ServiceType.JAKLINGKO, r.displayServiceType); assertEquals(ServiceType.BUS, r.serviceType) }
    @Test fun corridorCodesMatchWeb() { listOf("1", "13", "1A").forEach { assertEquals(ServiceType.BRT, route(it).displayServiceType) } }
    @Test fun otherCodesUseBusCategory() { assertEquals(ServiceType.BUS, route("D21").displayServiceType) }
}

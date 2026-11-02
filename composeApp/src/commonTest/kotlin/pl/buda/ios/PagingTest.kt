package pl.buda.ios
import kotlin.test.*
import kotlinx.coroutines.test.runTest
class PagingTest {
 @Test fun followsCursorAndDeduplicates()=runTest {
  val cursors=mutableListOf<Int>()
  val result=collectPages(2,{ x:Int->x }) { cursor -> cursors+=cursor;when(cursor){Int.MIN_VALUE->listOf(1,2);2->listOf(2,3);else->emptyList()} }
  assertEquals(listOf(1,2,3),result)
  assertEquals(listOf(Int.MIN_VALUE,2,3),cursors)
 }
 @Test fun refusesRepeatingFullPage()=runTest {
  assertFailsWith<IllegalStateException> { collectPages(2,{ x:Int->x }) { listOf(1,2) } }
 }
}

package nl.tudelft.ewi.se.ciselab.testgenie.services

import org.assertj.core.api.Assertions.assertThat
import org.evosuite.result.TestGenerationResultImpl
import org.evosuite.utils.CompactReport
import org.evosuite.utils.CompactTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestCaseCachingServiceTest {

    private lateinit var testCaseCachingService: TestCaseCachingService

    @BeforeEach
    fun setUp() {
        testCaseCachingService = TestCaseCachingService()
    }

    @Test
    fun singleFileSingleLine() {
        val report = CompactReport(TestGenerationResultImpl())
        val test1 = CompactTestCase("a", "aa", setOf(1, 2), setOf(), setOf())
        val test2 = CompactTestCase("b", "bb", setOf(2, 3), setOf(), setOf())
        report.testCaseList = hashMapOf(
            createPair(test1),
            createPair(test2)
        )

        val file = "file"

        testCaseCachingService.putIntoCache(file, report)

        val result = testCaseCachingService.retrieveFromCache(file, 2, 2)

        assertThat(result)
            .extracting<Triple<String, String, Set<Int>>> {
                Triple(it.testName.split(' ')[0], it.testCode, it.coveredLines)
            }
            .containsExactlyInAnyOrder(
                createTriple(test1),
                createTriple(test2)
            )
    }

    @Test
    fun singleFileMultipleLines() {
        val report = CompactReport(TestGenerationResultImpl())
        val test1 = CompactTestCase("a", "aa", setOf(1, 2), setOf(), setOf())
        val test2 = CompactTestCase("b", "bb", setOf(2, 3), setOf(), setOf())
        val test3 = CompactTestCase("c", "cc", setOf(1, 4), setOf(), setOf())
        val test4 = CompactTestCase("d", "dd", setOf(8), setOf(), setOf())
        val test5 = CompactTestCase("e", "ee", setOf(11), setOf(), setOf())
        report.testCaseList = hashMapOf(
            createPair(test1),
            createPair(test2),
            createPair(test3),
            createPair(test4),
            createPair(test5)
        )

        val file = "file"

        testCaseCachingService.putIntoCache(file, report)

        val result = testCaseCachingService.retrieveFromCache(file, 4, 10)

        assertThat(result)
            .extracting<Triple<String, String, Set<Int>>> {
                Triple(it.testName.split(' ')[0], it.testCode, it.coveredLines)
            }
            .containsExactlyInAnyOrder(
                createTriple(test3),
                createTriple(test4)
            )
    }

    @Test
    fun multipleFiles() {
        val report = CompactReport(TestGenerationResultImpl())
        val test1 = CompactTestCase("a", "aa", setOf(1, 2), setOf(), setOf())
        val test2 = CompactTestCase("b", "bb", setOf(2, 3), setOf(), setOf())
        val test3 = CompactTestCase("c", "cc", setOf(1, 4), setOf(), setOf())
        val test4 = CompactTestCase("d", "dd", setOf(8), setOf(), setOf())
        val test5 = CompactTestCase("e", "ee", setOf(11), setOf(), setOf())
        report.testCaseList = hashMapOf(
            createPair(test1),
            createPair(test2),
            createPair(test3),
            createPair(test4),
            createPair(test5)
        )

        val report2 = CompactReport(TestGenerationResultImpl())
        report2.testCaseList = hashMapOf(
            createPair(CompactTestCase("0a", "aa", setOf(1, 2), setOf(), setOf())),
            createPair(CompactTestCase("0b", "bb", setOf(2, 3), setOf(), setOf())),
            createPair(CompactTestCase("0c", "cc", setOf(1, 4), setOf(), setOf())),
            createPair(CompactTestCase("0d", "dd", setOf(8), setOf(), setOf())),
            createPair(CompactTestCase("0e", "ee", setOf(11), setOf(), setOf()))
        )

        val file = "file"

        testCaseCachingService.putIntoCache(file, report)
        testCaseCachingService.putIntoCache("file 2", report2)

        val result = testCaseCachingService.retrieveFromCache(file, 4, 10)

        assertThat(result)
            .extracting<Triple<String, String, Set<Int>>> {
                Triple(it.testName.split(' ')[0], it.testCode, it.coveredLines)
            }
            .containsExactlyInAnyOrder(
                createTriple(test3),
                createTriple(test4)
            )
    }

    @Test
    fun multipleFilesMultipleInsertions() {
        val report = CompactReport(TestGenerationResultImpl())
        val test1 = CompactTestCase("a", "aa", setOf(1, 2), setOf(), setOf())
        val test2 = CompactTestCase("b", "bb", setOf(2, 3), setOf(), setOf())
        val test3 = CompactTestCase("c", "cc", setOf(1, 4), setOf(), setOf())
        val test5 = CompactTestCase("e", "ee", setOf(11), setOf(), setOf())
        report.testCaseList = hashMapOf(
            createPair(test1),
            createPair(test2),
            createPair(test3),
            createPair(test5)
        )

        val report2 = CompactReport(TestGenerationResultImpl())
        report2.testCaseList = hashMapOf(
            createPair(CompactTestCase("0a", "aa", setOf(1, 2), setOf(), setOf())),
            createPair(CompactTestCase("0b", "bb", setOf(2, 3), setOf(), setOf())),
            createPair(CompactTestCase("0c", "cc", setOf(1, 4), setOf(), setOf())),
            createPair(CompactTestCase("0d", "dd", setOf(8), setOf(), setOf())),
            createPair(CompactTestCase("0e", "ee", setOf(11), setOf(), setOf()))
        )

        val report3 = CompactReport(TestGenerationResultImpl())
        val test4 = CompactTestCase("d", "dd", setOf(8), setOf(), setOf())
        report3.testCaseList = hashMapOf(
            createPair(test4)
        )

        val file = "file"

        testCaseCachingService.putIntoCache(file, report)
        testCaseCachingService.putIntoCache("file 2", report2)
        testCaseCachingService.putIntoCache(file, report3)

        val result = testCaseCachingService.retrieveFromCache(file, 4, 10)

        assertThat(result)
            .extracting<Triple<String, String, Set<Int>>> {
                Triple(it.testName.split(' ')[0], it.testCode, it.coveredLines)
            }
            .containsExactlyInAnyOrder(
                createTriple(test3),
                createTriple(test4)
            )
    }

    @Test
    fun nonexistentFile() {
        val report = CompactReport(TestGenerationResultImpl())
        val test1 = CompactTestCase("a", "aa", setOf(1, 2), setOf(), setOf())
        val test2 = CompactTestCase("b", "bb", setOf(2, 3), setOf(), setOf())
        report.testCaseList = hashMapOf(
            createPair(test1),
            createPair(test2)
        )

        testCaseCachingService.putIntoCache("aa", report)

        val result = testCaseCachingService.retrieveFromCache("bb", 2, 2)

        assertThat(result)
            .isEmpty()
    }

    @Test
    fun noMatchingTests() {
        val report = CompactReport(TestGenerationResultImpl())
        val test1 = CompactTestCase("a", "aa", setOf(1, 2), setOf(), setOf())
        val test2 = CompactTestCase("b", "bb", setOf(2, 3), setOf(), setOf())
        report.testCaseList = hashMapOf(
            createPair(test1),
            createPair(test2)
        )

        val file = "file"

        testCaseCachingService.putIntoCache(file, report)

        val result = testCaseCachingService.retrieveFromCache(file, 4, 50)

        assertThat(result)
            .isEmpty()
    }

    @Test
    fun invalidInputLines() {
        val report = CompactReport(TestGenerationResultImpl())
        val test1 = CompactTestCase("a", "aa", setOf(1, 2), setOf(), setOf())
        val test2 = CompactTestCase("b", "bb", setOf(2, 3), setOf(), setOf())
        report.testCaseList = hashMapOf(
            createPair(test1),
            createPair(test2)
        )

        val file = "file"

        testCaseCachingService.putIntoCache(file, report)

        val result = testCaseCachingService.retrieveFromCache(file, 4, 1)

        assertThat(result)
            .isEmpty()
    }

    private fun createPair(testCase: CompactTestCase): Pair<String, CompactTestCase> {
        return Pair(testCase.testName, testCase)
    }

    private fun createTriple(testCase: CompactTestCase): Triple<String, String, Set<Int>> {
        return Triple(testCase.testName, testCase.testCode, testCase.coveredLines)
    }
}
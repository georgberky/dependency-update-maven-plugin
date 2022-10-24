package io.github.georgberky.maven.plugins.depsupdate

import org.assertj.core.api.Assertions.assertThat
import org.jsoup.nodes.Document
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class VersionUpdateTest {
    @ParameterizedTest
    @MethodSource("sameVersionUpdates")
    fun `is latest version returns true if version numbers are the same`(current: String, latest: String) {
        val versionUpdate = object : VersionUpdate("someGroupId", "someArtifactId", current, latest) {
            override fun updatedPom(): Document {
                throw Exception("should not be called")
            }
        }

        assertThat(versionUpdate.isAlreadyLatestVersion())
            .isTrue()
    }

    @ParameterizedTest
    @MethodSource("differentVersionUpdates")
    fun `is latest version returns false if version numbers differ`(current: String, latest: String) {
        val versionUpdate = object : VersionUpdate("someGroupId", "someArtifactId", current, latest) {
            override fun updatedPom(): Document {
                throw Exception("should not be called")
            }
        }

        assertThat(versionUpdate.isAlreadyLatestVersion())
            .isFalse()
    }

    companion object {
        @JvmStatic
        fun sameVersionUpdates(): Stream<Arguments> {
            return Stream.of(
                arguments("1.0.0", "1.0.0"),
                arguments("1.1.0", "1.1.0"),
                arguments("1.0.1", "1.0.1")
            )
        }

        @JvmStatic
        fun differentVersionUpdates(): Stream<Arguments> {
            return Stream.of(
                arguments("1.0.0", "2.0.0"),
                arguments("1.1.0", "2.1.0"),
                arguments("1.0.1", "2.0.1")
            )
        }
    }
}

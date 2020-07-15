object VersionUtils {
    fun isTaggedRelease() = !System.getenv()["TRAVIS_TAG"].isNullOrBlank()
}
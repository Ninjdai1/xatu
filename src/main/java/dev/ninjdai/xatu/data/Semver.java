package dev.ninjdai.xatu.data;

import java.util.Arrays;
import java.util.List;

public class Semver {
    public final int major;
    public final int minor;
    public final int patch;

    public Semver(String versionString) {
        List<Integer> versionsList = Arrays.stream(versionString.split("\\.")).map(Integer::parseInt).toList();
        this.major = versionsList.get(0);
        this.minor = versionsList.get(1);
        this.patch = versionsList.get(2);
    }

    public Semver(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    @Override
    public String toString() {
        return String.format("%d.%d.%d", major, minor, patch);
    }

    public Semver nextPatch() {
        return new Semver(major, minor, patch+1);
    }

    public Semver nextMinor() {
        return new Semver(major, minor+1, 0);
    }

    public Semver nextMajor() {
        return new Semver(major+1, 0, 0);
    }
}

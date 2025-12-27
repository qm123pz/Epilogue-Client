package epilogue.ncm.music;

public enum Quality {
    STANDARD("Standard"),
    HIGHER("Higher"),
    EXHIGH("ExHigh"),
    LOSSLESS("LossLess"),
    HIRES("HiRes"),
    JYEFFECT("JyEffect"),
    SKY("sky"),
    JYMASTER("JyMaster");

    private final String quality;

    Quality(String quality) {
        this.quality = quality;
    }

    public String getQuality() {
        return quality;
    }
}

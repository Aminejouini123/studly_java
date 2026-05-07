package models;

public class Recommendation {


    public enum Type { JOB, COURSE }

    private final String title;
    private final String provider;
    private final String url;
    private final Type   type;
    private final int    matchScore;

    public Recommendation(String title, String provider, String url, Type type, int matchScore) {
        this.title      = title;
        this.provider   = provider;
        this.url        = url;
        this.type       = type;
        this.matchScore = matchScore;
    }

    public String getTitle()     { return title; }
    public String getProvider()  { return provider; }
    public String getUrl()       { return url; }
    public Type   getType()      { return type; }
    public int    getMatchScore(){ return matchScore; }
}
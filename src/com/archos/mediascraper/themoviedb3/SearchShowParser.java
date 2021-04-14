// Copyright 2020 Courville Software
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.archos.mediascraper.themoviedb3;

import android.os.Bundle;
import android.util.Pair;

import com.archos.mediascraper.SearchResult;
import com.archos.mediascraper.ShowUtils;
import com.archos.mediascraper.preprocess.TvShowSearchInfo;
import com.archos.mediascraper.xml.ShowScraper4;
import com.uwetrottmann.tmdb2.entities.BaseTvShow;
import com.uwetrottmann.tmdb2.entities.TvShowResultsPage;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import retrofit2.Response;

public class SearchShowParser {

    private static final Logger log = LoggerFactory.getLogger(SearchShowParser.class);
    private final static int SERIES_NOT_PERMITTED_ID = 313081;

    private final static LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

    private static List<SearchResult> normalAdd(SearchShowParserResult searchShowParserResult, int maxItems) {
        List<SearchResult> results = new LinkedList<>();
        if (searchShowParserResult.resultsProbable.size()>0)
            for (Pair<SearchResult,Integer> pair : searchShowParserResult.resultsProbable)
                if (maxItems < 0 || results.size() < maxItems)
                    results.add(pair.first);
        if (searchShowParserResult.resultsNumericSlug.size()>0)
            for (SearchResult result : searchShowParserResult.resultsNumericSlug)
                if (maxItems < 0 || results.size() < maxItems)
                    results.add(result);
        // skip shows without a poster
        /*
        if (searchShowParserResult.resultsNoPoster.size()>0)
            for (SearchResult result : searchShowParserResult.resultsNoPoster)
                if (maxItems < 0 || results.size() < maxItems)
                    results.add(result);
         */
        // skip shows without a banner
        /*
        if (searchShowParserResult.resultsNoBanner.size()>0)
            for (SearchResult result : searchShowParserResult.resultsNoBanner)
                if (maxItems < 0 || results.size() < maxItems)
                    results.add(result);
         */
        return results;
    }

    private static List<SearchResult> reSortAdd(SearchShowParserResult searchShowParserResult,
                                                 SearchShowParserResult globalSearchShowParserResult, int maxItems) {
        // if en has a lower Levenshtein distance than language, en has the right order: re-sort language list accordingly
        List<SearchResult> results = new LinkedList<>();
        if (searchShowParserResult.resultsProbable.size()>0
            && globalSearchShowParserResult.resultsProbable.size()>0)
            for (Pair<SearchResult, Integer> globalPair : globalSearchShowParserResult.resultsProbable)
                for (Pair<SearchResult, Integer> pair : searchShowParserResult.resultsProbable)
                    if (pair.first.getId() == globalPair.first.getId())
                        if (maxItems < 0 || results.size() < maxItems)
                            results.add(pair.first);
        if (searchShowParserResult.resultsNumericSlug.size()>0)
            for (SearchResult result : searchShowParserResult.resultsNumericSlug)
                if (maxItems < 0 || results.size() < maxItems)
                    results.add(result);
        // skip shows without a poster
        /*
        if (searchShowParserResult.resultsNoPoster.size()>0)
            for (SearchResult result : searchShowParserResult.resultsNoPoster)
                if (maxItems < 0 || results.size() < maxItems)
                    results.add(result);
         */
        // skip shows without a banner
        /*
        if (searchShowParserResult.resultsNoBanner.size()>0)
            for (SearchResult result : searchShowParserResult.resultsNoBanner)
                if (maxItems < 0 || results.size() < maxItems)
                    results.add(result);
         */
        return results;
    }

    public static List<SearchResult> getResult(Response<TvShowResultsPage> response,
                                               Response<TvShowResultsPage> globalResponse,
                                               TvShowSearchInfo searchInfo, String language,
                                               Integer maxItems, ShowScraper4 showScraper) {
        List<SearchResult> results = new LinkedList<>();
        SearchShowParserResult searchShowParserResult = new SearchShowParserResult();
        SearchShowParserResult globalSearchShowParserResult = new SearchShowParserResult();
        if (response != null) {
            searchShowParserResult = getSearchShowParserResult(response, searchInfo, language, showScraper);
        }
        if (!language.equals("en")) { // language != en
            log.debug("getResult: language is not en");
            if (globalResponse != null) {
                log.debug("getResult: globalResponse is not null");
                globalSearchShowParserResult = getSearchShowParserResult(globalResponse, searchInfo, "en", showScraper);
                if (searchShowParserResult.resultsProbable.size()>0
                        && globalSearchShowParserResult.resultsProbable.size()>0) {
                    log.debug("getResult: both searchShowParserResult.resultsProbable and globalSearchShowParserResult.resultsProbable available");
                    // if en has a lower Levenshtein distance than language, en has the right order: re-sort language list accordingly
                    if (globalSearchShowParserResult.resultsProbable.get(0).second <
                            searchShowParserResult.resultsProbable.get(0).second) {
                        log.debug("en has lower Levenshtein distance than " + language + ", re-sort fr based on en");
                        results = reSortAdd(searchShowParserResult, globalSearchShowParserResult, maxItems);
                    } else {
                        log.debug("getResult: en does not have better Levenshtein distance: use searchShowParserResult");
                        results = normalAdd(searchShowParserResult, maxItems);
                    }
                } else { // one of the two has size 0
                    log.debug("getResult: either searchShowParserResult.resultsProbable or globalSearchShowParserResult.resultsProbable is of size null");
                    if (response != null) { // use searchShowParserResult
                        log.debug("getResult: use searchShowParserResult");
                        results = normalAdd(searchShowParserResult, maxItems);
                    }
                    else { // revert to globalSearchShowParserResult
                        log.debug("getResult: use globalSearchShowParserResult");
                        results = normalAdd(globalSearchShowParserResult, maxItems);
                    }
                }
            } else { // globalResponse is null
                log.debug("getResult: globalResponse is null use searchShowParserResult");
                results = normalAdd(searchShowParserResult, maxItems);
            }
        } else { // language == en
            log.debug("getResult: language is en use searchShowParserResult");
            results = normalAdd(searchShowParserResult, maxItems);
        }
        return results;
    }

    private static SearchShowParserResult getSearchShowParserResult(Response<TvShowResultsPage> response,
                                                                    TvShowSearchInfo searchInfo, String language, ShowScraper4 showScraper) {
        SearchShowParserResult searchShowParserResult = new SearchShowParserResult();
        Boolean isDecisionTaken = false;
        log.debug("SearchShowParserResult: examining response of " + response.body().total_results + " entries in " + language + ", for " + searchInfo.getShowName());
        for (BaseTvShow series : response.body().results) {
            if (series.id != SERIES_NOT_PERMITTED_ID) {
                Bundle extra = new Bundle();
                extra.putString(ShowUtils.EPNUM, String.valueOf(searchInfo.getEpisode()));
                extra.putString(ShowUtils.SEASON, String.valueOf(searchInfo.getSeason()));
                SearchResult result = new SearchResult();
                result.setId(series.id);
                result.setLanguage(language);
                result.setTitle(series.name);
                log.debug("SearchShowParserResult: examining " + series.name + ", in " + language);
                result.setScraper(showScraper);
                result.setFile(searchInfo.getFile());
                result.setExtra(extra);
                // Put in lower priority any entry that has no TV show banned i.e. .*missing/movie.jpg as banner
                isDecisionTaken = false;
                if (series.backdrop_path != null) {
                    // TODO adapt to tmdb missing banner/poster
                    if (series.backdrop_path.endsWith("missing/series.jpg") || series.backdrop_path.endsWith("missing/movie.jpg") || series.backdrop_path == "") {
                        log.debug("getMatches2: set aside " + series.name + " because banner missing i.e. banner=" + series.backdrop_path);
                        searchShowParserResult.resultsNoBanner.add(result);
                        isDecisionTaken = true;
                    } else {
                        log.debug("getResult: " + series.name + " has backdrop_path " + series.backdrop_path);
                        result.setBackdropPath(series.backdrop_path);
                    }
                }
                if (series.poster_path != null) {
                    // TODO adapt to tmdb missing banner/poster
                    if (series.poster_path.endsWith("missing/series.jpg") || series.poster_path.endsWith("missing/movie.jpg") || series.poster_path == "") {
                        log.debug("getResult: set aside " + series.name + " because poster missing i.e. image=" + series.poster_path);
                        searchShowParserResult.resultsNoPoster.add(result);
                        isDecisionTaken = true;
                    } else {
                        log.debug("getResult: " + series.name + " has poster_path " + series.poster_path);
                        result.setBackdropPath(series.poster_path);
                    }
                }
                if (! isDecisionTaken) {
                    log.debug("getResult: taking into account " + series.name + " because banner/image exists");
                    isDecisionTaken = true;
                    searchShowParserResult.resultsProbable.add(new Pair<>(result,
                            levenshteinDistance.apply(searchInfo.getShowName().toLowerCase(),
                                    result.getTitle().toLowerCase())));
                }
                if (! isDecisionTaken)
                    log.warn("getSearchShowParserResult: ignore serie since banner/image is null for " + series.name);
            }
        }
        // TODO MARC levenstein metric on original title, no need to search in two languages... with tmdb perhaps
        log.debug("getResult: resultsProbable=" + searchShowParserResult.resultsProbable.toString());
        Collections.sort(searchShowParserResult.resultsProbable, new Comparator<Pair<SearchResult, Integer>>() {
            @Override
            public int compare(final Pair<SearchResult, Integer> sr1, final Pair<SearchResult, Integer> sr2) {
                if (sr1.second < sr2.second) {
                    return -1;
                } else if (sr1.second.equals(sr2.second)) {
                    return 0;
                } else {
                    return 1;
                }
            }
        });
        log.debug("getResult: applying Levenshtein distance resultsProbableSorted=" + searchShowParserResult.resultsProbable.toString());
        return searchShowParserResult;
    }
}
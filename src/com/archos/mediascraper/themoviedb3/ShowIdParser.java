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

import android.content.Context;
import android.util.Log;

import com.archos.medialib.R;
import com.archos.mediascraper.ShowTags;
import com.uwetrottmann.thetvdb.entities.Series;
import com.uwetrottmann.thetvdb.entities.SeriesResponse;
import com.uwetrottmann.tmdb2.entities.CastMember;
import com.uwetrottmann.tmdb2.entities.CrewMember;
import com.uwetrottmann.tmdb2.entities.Genre;
import com.uwetrottmann.tmdb2.entities.Network;
import com.uwetrottmann.tmdb2.entities.TvShow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ShowIdParser {
    private static final Logger log = LoggerFactory.getLogger(ShowIdParser.class);

    private static final String DIRECTOR = "Director";

    private static Context mContext;

    public static ShowTags getResult(TvShow serie, Context context) {
        mContext = context;
        ShowTags result = new ShowTags();

        result.setPlot(serie.overview);
        result.setRating(serie.vote_average.floatValue());
        result.setTitle(serie.name);

        log.debug("getResult: found title=" + serie.name + ", genre " + serie.genres);

        result.setContentRating(serie.rating.toString());
        result.setImdbId(serie.external_ids.imdb_id);
        result.setOnlineId(serie.id);
        result.setGenres(getLocalizedGenres(serie.genres));

        for (Network network : serie.networks)
            result.addStudioIfAbsent(network.name, '|', ',');

        result.setPremiered(serie.first_air_date);

        if (serie.credits != null) {
            if (serie.credits.guest_stars != null)
                for (CastMember guestStar : serie.credits.guest_stars)
                    result.addActorIfAbsent(guestStar.name, guestStar.character);
            if (serie.credits.cast != null)
                for (CastMember actor : serie.credits.cast)
                    result.addActorIfAbsent(actor.name, actor.character);
            if (serie.credits.crew != null)
                for (CrewMember crew : serie.credits.crew)
                    if (crew.job == DIRECTOR)
                        result.addDirectorIfAbsent(crew.name);
        }

        return result;
    }

    // many genres are not translated on tmdb and localized request is returned in the local language making one to
    // one mapping difficult without changing all db structure --> revert to show trick which is the only way to cope
    // with fallback search in en is perfomed when localized search returns nothing
    private static List<String> getLocalizedGenres(List<Genre> genres) {
        ArrayList<String> localizedGenres = new ArrayList<>();
        for (Genre genre : genres) {
            switch (genre.id) {
                case 10759:
                    localizedGenres.add(mContext.getString(R.string.tvshow_genre_action_adventure));
                    break;
                case 16:
                    localizedGenres.add(mContext.getString(R.string.tvshow_genre_animation));
                    break;
                case 35:
                    localizedGenres.add(mContext.getString(R.string.tvshow_genre_comedy));
                    break;
                case 80:
                    localizedGenres.add(mContext.getString(R.string.tvshow_genre_crime));
                    break;
                case 99:
                    localizedGenres.add(mContext.getString(R.string.tvshow_genre_documentary));
                    break;
                case 18:
                    localizedGenres.add(mContext.getString(R.string.tvshow_genre_drama));
                    break;
                case 10751:
                    localizedGenres.add(mContext.getString(R.string.tvshow_genre_family));
                    break;
                case 10762:
                    localizedGenres.add(mContext.getString(R.string.tvshow_genre_kids));
                    break;
                case 9648:
                    localizedGenres.add(mContext.getString(R.string.tvshow_genre_mystery));
                    break;
                case 10763:
                    localizedGenres.add(mContext.getString(R.string.tvshow_genre_news));
                    break;
                case 10764:
                    localizedGenres.add(mContext.getString(R.string.tvshow_genre_reality));
                    break;
                case 10765:
                    localizedGenres.add(mContext.getString(R.string.tvshow_genre_scifi_fantasy));
                    break;
                case 10766:
                    localizedGenres.add(mContext.getString(R.string.tvshow_genre_soap));
                    break;
                case 10767:
                    localizedGenres.add(mContext.getString(R.string.tvshow_genre_talk));
                    break;
                case 10768:
                    localizedGenres.add(mContext.getString(R.string.tvshow_genre_war_politics));
                    break;
                case 37:
                    localizedGenres.add(mContext.getString(R.string.tvshow_genre_western));
                    break;
                default:
                    log.warn("unknown genre: id=" + genre.id + ", name=" + genre.name);
                    localizedGenres.add(genre.name);
            }
        }
        return localizedGenres;
    }
}

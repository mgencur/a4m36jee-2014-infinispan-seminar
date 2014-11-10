/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package cz.ctu.fee.a4m36jee.seminar.infinispan.session;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.inject.Model;
import javax.inject.Inject;
import javax.transaction.UserTransaction;

import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.infinispan.Cache;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.query.CacheQuery;
import org.infinispan.query.Search;
import org.infinispan.query.SearchManager;

import cz.ctu.fee.a4m36jee.seminar.infinispan.model.Car;

/**
 * Adds, retrieves, removes new cars from the cache. Also returns a list of cars stored in the cache.
 *
 * @author Martin Gencur
 */
@Model
public class CarManager {

    public static final String CAR_CACHE_NAME = "carcache";
    public static final String CAR_LIST_CACHE_NAME = "carlist";
    public static final String CAR_NUMBERS_KEY = "carnumbers";

    @Inject
    private CacheContainerProvider provider;

    @Inject
    private UserTransaction utx;

    private BasicCache<String, Object> carCache;
    private BasicCache<String, Object> carListCache;
    private List<Car> searchResults;
    private String carId;
    private Car car = new Car();
    private boolean matchAll;

    public CarManager() {
    }

    public String addNewCar() {
        carCache = provider.getCacheContainer().getCache(CAR_CACHE_NAME);
        carListCache = provider.getCacheContainer().getCache(CarManager.CAR_LIST_CACHE_NAME);

        try {
            utx.begin();

            List<String> carNumbers = getNumberPlateList(carListCache);
            carNumbers.add(car.getNumberPlate());

            carListCache.put(CAR_NUMBERS_KEY, carNumbers);
            carCache.put(CarManager.encode(car.getNumberPlate()), car);

            utx.commit();
        } catch (Exception e) {
            if (utx != null) {
                try {
                    utx.rollback();
                } catch (Exception e1) {
                }
            }
        }

        return "home";
    }

    /**
     * Operate on a clone of car number list
     */
    @SuppressWarnings("unchecked")
    private List<String> getNumberPlateList(BasicCache<String, Object> carListCacheLoc) {
        List<String> carNumberList = (List<String>) carListCacheLoc.get(CAR_NUMBERS_KEY);
        return carNumberList == null ? new LinkedList<>() : new LinkedList<>(carNumberList);
    }

    public String showCarDetails(String numberPlate) {
        carCache = provider.getCacheContainer().getCache(CAR_CACHE_NAME);
        this.car = (Car) carCache.get(encode(numberPlate));

        return "showdetails";
    }

    public List<String> getCarList() {
        // retrieve a cache
        carListCache = provider.getCacheContainer().getCache(CarManager.CAR_LIST_CACHE_NAME);
        // retrieve a list of number plates from the cache
        return getNumberPlateList(carListCache);
    }

    public String clearSearchResults() {
        searchResults = null;
        return "home";
    }

    public boolean isSearchActive() {
        return searchResults != null;
    }

    public String removeCar(String numberPlate) {
        carCache = provider.getCacheContainer().getCache(CAR_CACHE_NAME);
        carListCache = provider.getCacheContainer().getCache(CarManager.CAR_LIST_CACHE_NAME);

        try {
            utx.begin();

            carCache.remove(encode(numberPlate));

            List<String> carNumbers = getNumberPlateList(carListCache);
            carNumbers.remove(numberPlate);

            carListCache.put(CAR_NUMBERS_KEY, carNumbers);

            utx.commit();
        } catch (Exception e) {
            if (utx != null) {
                try {
                    utx.rollback();
                } catch (Exception e1) {
                }
            }
        }

        return null;
    }

    public void setCarId(String carId) {
        this.carId = carId;
    }

    public String getCarId() {
        return carId;
    }

    public void setCar(Car car) {
        this.car = car;
    }

    public Car getCar() {
        return car;
    }

    public boolean isMatchAll() {
        return matchAll;
    }

    public void setMatchAll(boolean matchAll) {
        this.matchAll = matchAll;
    }

    public static String encode(String key) {
        try {
            return URLEncoder.encode(key, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String decode(String key) {
        try {
            return URLDecoder.decode(key, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public String search() {
        carCache = provider.getCacheContainer().getCache(CAR_CACHE_NAME);
        SearchManager sm = Search.getSearchManager((Cache) carCache);

        // match all / any of parameters
        Query q = matchAll ? createMatchAllQuery() : crateMatchAnyQuery(sm);

        // create a cache query based on the Lucene query
        CacheQuery cq = sm.getQuery(q, Car.class);

        // invoke the cache query
        searchResults = cq.list().stream().filter(o -> o instanceof Car).map(o -> (Car) o).collect(Collectors.toList());
        return "searchresults";
    }

    // TODO: create the query using BooleanQuery from Lucene
    private Query createMatchAllQuery() {
        BooleanQuery q = null;
        System.out.println("Lucene Query: " + q.toString() );

        return q;
    }

    // TODO: create the query using QueryBuilder from Hibernate
    private Query crateMatchAnyQuery(SearchManager sm) {
        QueryBuilder queryBuilder = null;
        Query q = null;
        System.out.println("Hibernate Query: " + q.toString() );

        return q;
    }

    public List<Car> getSearchResults() {
        return searchResults;
    }
}

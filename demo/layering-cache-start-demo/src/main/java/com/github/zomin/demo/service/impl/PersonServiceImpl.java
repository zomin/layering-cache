package com.github.zomin.demo.service.impl;

import com.github.zomin.annotation.*;
import com.github.zomin.demo.entity.Person;
import com.github.zomin.demo.service.PersonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PersonServiceImpl implements PersonService {
    Logger logger = LoggerFactory.getLogger(PersonServiceImpl.class);

    @Override
    @CachePut(value = "people", key = "#person.id", depict = "用户信息缓存")
    public Person save(Person person) {
        logger.info("为id、key为:" + person.getId() + "数据做了缓存");
        return person;
    }

    @Override
    @CacheEvict(value = "people", key = "#id")//2
    public void remove(Long id) {
        logger.info("删除了id、key为" + id + "的数据缓存");
        //这里不做实际删除操作
    }

    @Override
    @CacheEvict(value = "people", allEntries = true)//2
    public void removeAll() {
        logger.info("删除了所有缓存的数据缓存");
        //这里不做实际删除操作
    }

    @Override
    @Cacheable(value = "people", key = "#person.id", depict = "用户信息缓存",
            firstCache = @FirstCache(expireTime = 4),
            secondaryCache = @SecondaryCache(expireTime = 15, preloadTime = 8, forceRefresh = true))
    public Person findOne(Person person) {
        Person p = new Person(2L, "name2", 12,"address2");
        logger.info("为id、key为:" + p.getId() + "数据做了缓存");
        return p;
    }
}

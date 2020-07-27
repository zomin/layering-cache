package com.github.zomin.demo.service;


import com.github.zomin.demo.entity.Person;

public interface PersonService {
    Person save(Person person);

    void remove(Long id);

    void removeAll();

    Person findOne(Person person);
}

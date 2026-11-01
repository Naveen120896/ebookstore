package com.ebookstore.service;

import com.ebookstore.dto.Dtos.AddressDto;
import com.ebookstore.dto.Mapper;
import com.ebookstore.entity.Address;
import com.ebookstore.repository.AddressRepository;
import com.ebookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {
    private final AddressRepository addressRepo;
    private final UserRepository userRepo;

    @Transactional(readOnly = true)
    public List<AddressDto> list(Long userId) {
        return addressRepo.findByUserId(userId).stream().map(Mapper::address).toList();
    }

    @Transactional
    public AddressDto create(Long userId, AddressDto d) {
        Address a = new Address();
        a.setUser(userRepo.getReferenceById(userId));
        a.setLine1(d.line1());
        a.setLine2(d.line2());
        a.setCity(d.city());
        a.setState(d.state());
        a.setPostalCode(d.postalCode());
        a.setCountry(d.country());
        boolean makeDefault = addressRepo.countByUserId(userId) == 0 || Boolean.TRUE.equals(d.isDefault());
        if (makeDefault) {
            addressRepo.findByUserId(userId).forEach(x -> x.setDefaultAddress(false));
        }
        a.setDefaultAddress(makeDefault);
        return Mapper.address(addressRepo.save(a));
    }
}

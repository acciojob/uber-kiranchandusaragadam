package com.driver.services.impl;

import com.driver.exceptions.NoValuePresentException;
import com.driver.exceptions.NoValuePresentException;
import com.driver.model.TripBooking;
import com.driver.services.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.driver.model.*;
import com.driver.model.Driver;
import com.driver.repository.CustomerRepository;
import com.driver.repository.DriverRepository;
import com.driver.repository.TripBookingRepository;
import com.driver.model.TripStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class CustomerServiceImpl implements CustomerService {

	@Autowired
	CustomerRepository customerRepository2;

	@Autowired
	DriverRepository driverRepository2;

	@Autowired
	TripBookingRepository tripBookingRepository2;

	@Override
	public void register(Customer customer) {
		//Save the customer in database
		customerRepository2.save(customer);
	}

	@Override
	public void deleteCustomer(Integer customerId) {
		// Delete customer without using deleteById function
		customerRepository2.deleteCustomer(customerId);
	}

	@Override
	public TripBooking bookTrip(int customerId, String fromLocation, String toLocation, int distanceInKm) throws Exception{
		//Book the driver with lowest driverId who is free (cab available variable is Boolean.TRUE). If no driver is available, throw "No cab available!" exception
		//Avoid using SQL query
		TripBooking tripBooking = new TripBooking();
		tripBooking.setFromLocation(fromLocation);
		tripBooking.setToLocation(toLocation);
		tripBooking.setDistanceInKm(distanceInKm);
		tripBooking.setStatus(TripStatus.CONFIRMED);

		// link with customer
		Customer customer = customerRepository2.findById(customerId).get();
		tripBooking.setCustomer(customer);

		// find driver who is free
		Driver driver = null;
		int perKmRate = 0;
		List<Driver> driverList = driverRepository2.findAll();

		Collections.sort(driverList, (a, b) -> {
			return a.getDriverId() < b.getDriverId() ? -1 : 1;
		});

		for(Driver dr : driverList){
			// get that driver cab
			Cab cab = dr.getCab();
			if(cab.getAvailable()){
				cab.setAvailable(false);
				dr.setCab(cab);
				driverRepository2.save(dr);
				driver = dr;
				perKmRate = cab.getPerKmRate();
				break;
			}
		}

		if(driver != null){
			tripBooking.setDriver(driver);
			int bill = distanceInKm * perKmRate;
			tripBooking.setBill(bill);
			tripBookingRepository2.save(tripBooking);

			// save trip booking in customer database
			List<TripBooking> tripBookingList = customer.getTripBookingList();
			if(tripBookingList == null){
				tripBookingList = new ArrayList<>();
			}
			tripBookingList.add(tripBooking);
			customerRepository2.save(customer);

			return tripBooking;
		}
		else{
			throw new NoValuePresentException("No value present");
		}
	}

	@Override
	public void cancelTrip(Integer tripId){
		//Cancel the trip having given trip Id and update TripBooking attributes accordingly
		TripBooking tripBooking = tripBookingRepository2.findById(tripId).get();
		tripBooking.setStatus(TripStatus.CANCELED);
		tripBooking.setBill(0);

		// free up the cab
		Driver driver = tripBooking.getDriver();
		Cab cab = driver.getCab();
		cab.setAvailable(true);
		driver.setCab(cab);
		driverRepository2.save(driver);

		tripBookingRepository2.save(tripBooking);
	}

	@Override
	public void completeTrip(Integer tripId){
		//Complete the trip having given trip Id and update TripBooking attributes accordingly
		TripBooking tripBooking = tripBookingRepository2.findById(tripId).get();
		tripBooking.setStatus(TripStatus.COMPLETED);

		// free up the cab
		Driver driver = tripBooking.getDriver();
		Cab cab = driver.getCab();
		cab.setAvailable(true);
		driver.setCab(cab);
		driverRepository2.save(driver);

		tripBookingRepository2.save(tripBooking);
	}
}

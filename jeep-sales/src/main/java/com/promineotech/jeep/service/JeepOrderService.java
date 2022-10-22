
package com.promineotech.jeep.service;

import org.springframework.stereotype.Service;
import com.promineotech.jeep.entity.Order;
import com.promineotech.jeep.entity.OrderRequest;

@Service
public interface JeepOrderService {
  
  Order createOrder(OrderRequest orderRequest);
}

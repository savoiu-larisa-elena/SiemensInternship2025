package com.siemens.internship.controller;

import com.siemens.internship.model.Item;
import com.siemens.internship.service.ItemService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/items")
public class ItemController {
    private static final Logger logger = LoggerFactory.getLogger(ItemController.class);

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    /**
     * GET /api/items
     * Retrieves all items from the database.
     * @return 200 OK with the list of items
     */
    @GetMapping
    public ResponseEntity<List<Item>> getAllItems() {
        return ResponseEntity.ok(itemService.findAll());
    }

    /**
     * POST /api/items
     * Creates a new item. Validates input and handles errors.
     * @param item the item to be created
     * @param result the binding result for validation errors
     * @return 201 Created if valid, 400 Bad Request if validation fails
     */
    @PostMapping
    public ResponseEntity<Object> createItem(@Valid @RequestBody Item item, BindingResult result) {
        if (result.hasErrors()) {
            String message = Objects.requireNonNull(result.getFieldError()).getDefaultMessage();
            return ResponseEntity.badRequest().body("Invalid input: " + message);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(itemService.save(item));
    }

    /**
     * GET /api/items/{id}
     * Retrieves an item by ID.
     * @param id the ID of the item
     * @return 200 OK with the item, or 404 Not Found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Object> getItemById(@PathVariable Long id) {
        return itemService.findById(id)
                .<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Item not found"));
    }

    /**
     * PUT /api/items/{id}
     * Updates an existing item. Validates input and checks existence.
     * @param id the ID of the item to update
     * @param item the updated item data
     * @param result the binding result for validation errors
     * @return 200 OK if successful, 404 if item not found, 400 if invalid
     */
    @PutMapping("/{id}")
    public ResponseEntity<Object> updateItem(@PathVariable Long id, @Valid @RequestBody Item item, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(Objects.requireNonNull(result.getFieldError()).getDefaultMessage());
        }
        if (itemService.findById(id).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Item not found");
        }
        item.setId(id);
        return ResponseEntity.ok(itemService.save(item));
    }

    /**
     * DELETE /api/items/{id}
     * Deletes an item by ID.
     * @param id the ID of the item to delete
     * @return 200 OK if deleted, 404 Not Found if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteItem(@PathVariable Long id) {
        return itemService.findById(id)
                .map(item -> {
                    itemService.deleteById(id);
                    return ResponseEntity.ok("Item deleted successfully");
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Item not found"));
    }

    /**
     * GET /api/items/process
     * Triggers asynchronous processing of all items.
     * @return 200 OK with processed items or 500 if processing fails
     */
    @GetMapping("/process")
    public CompletableFuture<ResponseEntity<List<Item>>> processItems() {
        return itemService.processItemsAsync()
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    logger.error("Failed to process items asynchronously", ex);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });
    }
}
package com.siemens.internship.service;

import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;



@Service
public class ItemService {
    private static final Logger logger = LoggerFactory.getLogger(ItemService.class);

    private final ItemRepository itemRepository;
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);
    @Getter
    @Setter
    private List<Item> processedItems = new ArrayList<>();
    @Getter
    private int processedCount = 0;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    /**
     * Retrieves all items from the database.
     * @return a list of all items
     */
    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    /**
     * Finds an item by its ID.
     * @param id the ID of the item
     * @return an Optional containing the item if found, otherwise empty
     */
    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    /**
     * Saves an item to the database.
     * @param item the item to save
     * @return the saved item
     */
    public Item save(Item item) {
        return itemRepository.save(item);
    }

    /**
     * Deletes an item by its ID.
     * @param id the ID of the item to delete
     */
    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }


    /**
     * Your Tasks
     * Identify all concurrency and asynchronous programming issues in the code
     * Fix the implementation to ensure:
     * All items are properly processed before the CompletableFuture completes
     * Thread safety for all shared state
     * Proper error handling and propagation
     * Efficient use of system resources
     * Correct use of Spring's @Async annotation
     * Add appropriate comments explaining your changes and why they fix the issues
     * Write a brief explanation of what was wrong with the original implementation
     * Hints
     * Consider how CompletableFuture composition can help coordinate multiple async operations
     * Think about appropriate thread-safe collections
     * Examine how errors are handled and propagated
     * Consider the interaction between Spring's @Async and CompletableFuture
     */

    /**
     * Asynchronously processes all items:
     * - Retrieves all item IDs
     * - Loads and updates each item in parallel using a thread pool
     * - Sets their status to "PROCESSED" and saves them back to the database
     * - Tracks and returns a list of successfully processed items
     *
     * @return a CompletableFuture containing the list of processed items
     */
    @Async
    public CompletableFuture<List<Item>> processItemsAsync() {
        List<Long> itemIds = itemRepository.findAllIds();
        List<Item> processedItems = new CopyOnWriteArrayList<>();

        List<CompletableFuture<Void>> futures = itemIds.stream()
                .map(id -> CompletableFuture.runAsync(()-> {
                    try {
                        Thread.sleep(150);

                        Optional<Item> optionalItem = itemRepository.findById(id);
                        if(optionalItem.isEmpty())
                            return;

                        Item item = optionalItem.get();
                        item.setStatus("PROCESSED");
                        itemRepository.save(item);
                        processedItems.add(item);
                    } catch (Exception e) {
                        logger.error("Error processing item with ID: {}", id, e);
                    }
        }, executor)).toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenApply(v -> processedItems);
    }
}


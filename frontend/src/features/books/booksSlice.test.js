import { describe, expect, test } from "vitest";
import booksReducer, {
  checkBookAvailability,
  deleteBook,
  fetchBooks,
} from "./booksSlice";

describe("booksSlice reducer", () => {
  test("fetchBooks.fulfilled should store the paginated books", () => {
    const initialState = undefined;

    const responseData = {
      content: [
        {
          id: 1,
          title: "Clean Code",
          author: "Robert C. Martin",
          isbn: "9780132350884",
          publishedYear: 2008,
        },
        {
          id: 2,
          title: "Effective Java",
          author: "Joshua Bloch",
          isbn: "9780134685991",
          publishedYear: 2018,
        },
      ],
      number: 0,
      size: 5,
      totalPages: 1,
      totalElements: 2,
    };

    const action = fetchBooks.fulfilled(responseData, "request-id", {
      search: "",
      page: 0,
      size: 5,
    });

    const newState = booksReducer(initialState, action);

    expect(newState.items).toEqual(responseData.content);
    expect(newState.page).toBe(0);
    expect(newState.size).toBe(5);
    expect(newState.totalPages).toBe(1);
    expect(newState.totalElements).toBe(2);
    expect(newState.status).toBe("succeeded");
    expect(newState.error).toBeNull();
  });

  test("deleteBook.fulfilled should remove the deleted book and its availability", () => {
    const previousState = {
      items: [
        {
          id: 1,
          title: "Clean Code",
        },
        {
          id: 2,
          title: "Effective Java",
        },
      ],
      status: "succeeded",
      error: null,
      page: 0,
      size: 5,
      totalPages: 1,
      totalElements: 2,
      actionStatus: "idle",
      actionError: null,
      availabilityByBookId: {
        1: {
          available: false,
          remainingDays: 5,
        },
        2: {
          available: true,
          remainingDays: null,
        },
      },
    };

    const action = deleteBook.fulfilled(1, "request-id", 1);

    const newState = booksReducer(previousState, action);

    expect(newState.items).toEqual([
      {
        id: 2,
        title: "Effective Java",
      },
    ]);

    expect(newState.availabilityByBookId[1]).toBeUndefined();
    expect(newState.availabilityByBookId[2]).toEqual({
      available: true,
      remainingDays: null,
    });

    expect(newState.actionStatus).toBe("succeeded");
    expect(newState.actionError).toBeNull();
  });

  test("fetchBooks.rejected should store the loading error", () => {
    const initialState = undefined;

    const action = fetchBooks.rejected(
      new Error("Failed to fetch books"),
      "request-id",
      {
        search: "",
        page: 0,
        size: 5,
      },
    );

    const newState = booksReducer(initialState, action);

    expect(newState.status).toBe("failed");
    expect(newState.error).toBe("Failed to fetch books");
  });

  test("checkBookAvailability.rejected should store the action error", () => {
    const previousState = {
      items: [],
      status: "idle",
      error: null,
      page: 0,
      size: 5,
      totalPages: 0,
      totalElements: 0,
      actionStatus: "idle",
      actionError: null,
      availabilityByBookId: {},
    };

    const action = checkBookAvailability.rejected(
      new Error("Failed to check availability"),
      "request-id",
      1,
    );

    const newState = booksReducer(previousState, action);

    expect(newState.actionError).toBe("Failed to check availability");
  });
});

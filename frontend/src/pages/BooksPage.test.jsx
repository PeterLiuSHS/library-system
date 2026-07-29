import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import {
  afterEach,
  beforeEach,
  describe,
  expect,
  test,
  vi,
} from "vitest";
import { useDispatch, useSelector } from "react-redux";
import BooksPage from "./BooksPage";

const {
  mockCheckBookAvailability,
  mockClearBookActionError,
  mockCreateBook,
  mockDeleteBook,
  mockFetchBooks,
  mockUpdateBook,
} = vi.hoisted(() => ({
  mockCheckBookAvailability: vi.fn(),
  mockClearBookActionError: vi.fn(),
  mockCreateBook: vi.fn(),
  mockDeleteBook: vi.fn(),
  mockFetchBooks: vi.fn(),
  mockUpdateBook: vi.fn(),
}));

vi.mock("react-redux", () => ({
  useDispatch: vi.fn(),
  useSelector: vi.fn(),
}));

vi.mock("../features/books/booksSlice", () => ({
  checkBookAvailability: mockCheckBookAvailability,
  clearBookActionError: mockClearBookActionError,
  createBook: mockCreateBook,
  deleteBook: mockDeleteBook,
  fetchBooks: mockFetchBooks,
  updateBook: mockUpdateBook,
}));

describe("BooksPage", () => {
  const mockDispatch = vi.fn();

  const book1 = {
    id: 1,
    title: "Clean Code",
    author: "Robert C. Martin",
    isbn: "9780132350884",
    publishedYear: 2008,
    createdAt: "2026-07-01T10:00:00",
  };

  const book2 = {
    id: 2,
    title: "Effective Java",
    author: "Joshua Bloch",
    isbn: "9780134685991",
    publishedYear: 2018,
    createdAt: "2026-07-02T10:00:00",
  };

  const mockState = {
    books: {
      items: [book1, book2],
      status: "succeeded",
      error: null,
      page: 0,
      size: 5,
      totalPages: 1,
      totalElements: 2,
      actionStatus: "idle",
      actionError: null,
      availabilityByBookId: {},
    },
  };

  function setMockState(state) {
    useSelector.mockImplementation((selector) => selector(state));
  }

  beforeEach(() => {
    vi.clearAllMocks();

    useDispatch.mockReturnValue(mockDispatch);
    setMockState(mockState);

    mockClearBookActionError.mockReturnValue({
      type: "books/clearBookActionError",
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  test("should display books from Redux state", () => {
    render(<BooksPage />);

    expect(
      screen.getByRole("heading", { name: "Books" }),
    ).toBeInTheDocument();

    expect(screen.getByText("Total books: 2")).toBeInTheDocument();

    expect(screen.getByText("Clean Code")).toBeInTheDocument();
    expect(
      screen.getByText("Robert C. Martin"),
    ).toBeInTheDocument();

    expect(screen.getByText("Effective Java")).toBeInTheDocument();
    expect(screen.getByText("Joshua Bloch")).toBeInTheDocument();

    expect(screen.getByText("9780132350884")).toBeInTheDocument();
    expect(screen.getByText("2008")).toBeInTheDocument();
  });

  test("should fetch the first page when status is idle", () => {
    const idleState = {
      books: {
        ...mockState.books,
        items: [],
        status: "idle",
        totalElements: 0,
      },
    };

    const fetchBooksAction = {
      type: "books/fetchBooks/pending",
    };

    setMockState(idleState);
    mockFetchBooks.mockReturnValue(fetchBooksAction);

    render(<BooksPage />);

    expect(mockFetchBooks).toHaveBeenCalledWith({
      search: "",
      page: 0,
      size: 5,
    });

    expect(mockDispatch).toHaveBeenCalledWith(fetchBooksAction);
  });

  test("should search books using the entered keyword", async () => {
    const user = userEvent.setup();

    const clearErrorAction = {
      type: "books/clearBookActionError",
    };

    const fetchBooksAction = {
      type: "books/fetchBooks/pending",
    };

    mockClearBookActionError.mockReturnValue(clearErrorAction);
    mockFetchBooks.mockReturnValue(fetchBooksAction);

    render(<BooksPage />);

    const searchInput = screen.getByPlaceholderText(
      "Search by title or author",
    );

    await user.type(searchInput, "java");

    await user.click(
      screen.getByRole("button", {
        name: "Search",
      }),
    );

    expect(searchInput).toHaveValue("java");

    expect(mockClearBookActionError).toHaveBeenCalledTimes(1);

    expect(mockFetchBooks).toHaveBeenCalledWith({
      search: "java",
      page: 0,
      size: 5,
    });

    expect(mockDispatch).toHaveBeenCalledWith(clearErrorAction);
    expect(mockDispatch).toHaveBeenCalledWith(fetchBooksAction);
  });

  test("should clear the search input and fetch all books", async () => {
    const user = userEvent.setup();

    const fetchBooksAction = {
      type: "books/fetchBooks/pending",
    };

    mockFetchBooks.mockReturnValue(fetchBooksAction);

    render(<BooksPage />);

    const searchInput = screen.getByPlaceholderText(
      "Search by title or author",
    );

    await user.type(searchInput, "java");

    expect(searchInput).toHaveValue("java");

    await user.click(
      screen.getByRole("button", {
        name: "Clear",
      }),
    );

    expect(searchInput).toHaveValue("");

    expect(mockClearBookActionError).toHaveBeenCalledTimes(1);

    expect(mockFetchBooks).toHaveBeenCalledWith({
      search: "",
      page: 0,
      size: 5,
    });

    expect(mockDispatch).toHaveBeenCalledWith(fetchBooksAction);
  });

  test("should check book availability when Check is clicked", async () => {
    const user = userEvent.setup();

    const checkAvailabilityAction = {
      type: "books/checkBookAvailability/pending",
    };

    mockCheckBookAvailability.mockReturnValue(
      checkAvailabilityAction,
    );

    render(<BooksPage />);

    const checkButtons = screen.getAllByRole("button", {
      name: "Check",
    });

    await user.click(checkButtons[0]);

    expect(
      mockCheckBookAvailability,
    ).toHaveBeenCalledWith(1);

    expect(mockDispatch).toHaveBeenCalledWith(
      checkAvailabilityAction,
    );
  });

  test("should display available status from Redux state", () => {
    const availabilityState = {
      books: {
        ...mockState.books,
        availabilityByBookId: {
          1: {
            available: true,
          },
        },
      },
    };

    setMockState(availabilityState);

    render(<BooksPage />);

    expect(screen.getByText("Available")).toBeInTheDocument();
  });

  test("should display unavailable status from Redux state", () => {
    const availabilityState = {
      books: {
        ...mockState.books,
        availabilityByBookId: {
          1: {
            available: false,
            remainingDays: 3,
          },
        },
      },
    };

    setMockState(availabilityState);

    render(<BooksPage />);

    expect(
      screen.getByText(
        "Not available, 3 day(s) remaining",
      ),
    ).toBeInTheDocument();
  });

  test("should create a book and refresh the first page", async () => {
    const user = userEvent.setup();

    const createBookAction = {
      type: "books/createBook/pending",
    };

    const fetchBooksAction = {
      type: "books/fetchBooks/pending",
    };

    const unwrap = vi.fn().mockResolvedValue({
      id: 3,
    });

    mockCreateBook.mockReturnValue(createBookAction);
    mockFetchBooks.mockReturnValue(fetchBooksAction);

    mockDispatch.mockImplementation((action) => {
      if (action === createBookAction) {
        return {
          unwrap,
        };
      }

      return action;
    });

    render(<BooksPage />);

    const titleInput =
      screen.getByPlaceholderText("Title");

    const authorInput =
      screen.getByPlaceholderText("Author");

    const isbnInput =
      screen.getByPlaceholderText("ISBN");

    const yearInput =
      screen.getByPlaceholderText("Published Year");

    await user.type(titleInput, "Spring in Action");
    await user.type(authorInput, "Craig Walls");
    await user.type(isbnInput, "9781617297571");
    await user.type(yearInput, "2022");

    await user.click(
      screen.getByRole("button", {
        name: "Add Book",
      }),
    );

    expect(mockCreateBook).toHaveBeenCalledWith({
      title: "Spring in Action",
      author: "Craig Walls",
      isbn: "9781617297571",
      publishedYear: 2022,
    });

    expect(mockDispatch).toHaveBeenCalledWith(
      createBookAction,
    );

    expect(unwrap).toHaveBeenCalledTimes(1);

    expect(mockFetchBooks).toHaveBeenCalledWith({
      search: "",
      page: 0,
      size: 5,
    });

    expect(titleInput).toHaveValue("");
    expect(authorInput).toHaveValue("");
    expect(isbnInput).toHaveValue("");
    expect(yearInput).toHaveValue(null);
  });

  test("should send null when published year is empty", async () => {
    const user = userEvent.setup();

    const createBookAction = {
      type: "books/createBook/pending",
    };

    const unwrap = vi.fn().mockResolvedValue({
      id: 3,
    });

    mockCreateBook.mockReturnValue(createBookAction);

    mockDispatch.mockImplementation((action) => {
      if (action === createBookAction) {
        return {
          unwrap,
        };
      }

      return action;
    });

    render(<BooksPage />);

    await user.type(
      screen.getByPlaceholderText("Title"),
      "Spring Boot Up and Running",
    );

    await user.type(
      screen.getByPlaceholderText("Author"),
      "Mark Heckler",
    );

    await user.type(
      screen.getByPlaceholderText("ISBN"),
      "9781492076988",
    );

    await user.click(
      screen.getByRole("button", {
        name: "Add Book",
      }),
    );

    expect(mockCreateBook).toHaveBeenCalledWith({
      title: "Spring Boot Up and Running",
      author: "Mark Heckler",
      isbn: "9781492076988",
      publishedYear: null,
    });
  });

  test("should only allow title and author to be edited", async () => {
    const user = userEvent.setup();

    render(<BooksPage />);

    const editButtons = screen.getAllByRole("button", {
      name: "Edit",
    });

    await user.click(editButtons[0]);

    expect(
      screen.getByDisplayValue("Clean Code"),
    ).toBeInTheDocument();

    expect(
      screen.getByDisplayValue("Robert C. Martin"),
    ).toBeInTheDocument();

    expect(
      screen.queryByDisplayValue("9780132350884"),
    ).not.toBeInTheDocument();

    expect(
      screen.queryByDisplayValue("2008"),
    ).not.toBeInTheDocument();

    expect(screen.getByText("9780132350884")).toBeInTheDocument();
    expect(screen.getByText("2008")).toBeInTheDocument();
  });

  test("should edit and update a book", async () => {
    const user = userEvent.setup();

    const updateBookAction = {
      type: "books/updateBook/pending",
    };

    const unwrap = vi.fn().mockResolvedValue({
      id: 1,
    });

    mockUpdateBook.mockReturnValue(updateBookAction);

    mockDispatch.mockImplementation((action) => {
      if (action === updateBookAction) {
        return {
          unwrap,
        };
      }

      return action;
    });

    render(<BooksPage />);

    const editButtons = screen.getAllByRole("button", {
      name: "Edit",
    });

    await user.click(editButtons[0]);

    const titleInput =
      screen.getByDisplayValue("Clean Code");

    const authorInput = screen.getByDisplayValue(
      "Robert C. Martin",
    );

    await user.clear(titleInput);
    await user.type(titleInput, "Clean Code Updated");

    await user.clear(authorInput);
    await user.type(authorInput, "Robert Martin");

    await user.click(
      screen.getByRole("button", {
        name: "Save",
      }),
    );

    expect(mockUpdateBook).toHaveBeenCalledWith({
      bookId: 1,
      title: "Clean Code Updated",
      author: "Robert Martin",
    });

    expect(mockDispatch).toHaveBeenCalledWith(
      updateBookAction,
    );

    expect(unwrap).toHaveBeenCalledTimes(1);

    expect(
      screen.queryByDisplayValue("Clean Code Updated"),
    ).not.toBeInTheDocument();
  });

  test("should cancel editing without updating the book", async () => {
    const user = userEvent.setup();

    render(<BooksPage />);

    const editButtons = screen.getAllByRole("button", {
      name: "Edit",
    });

    await user.click(editButtons[0]);

    expect(
      screen.getByDisplayValue("Clean Code"),
    ).toBeInTheDocument();

    await user.click(
      screen.getByRole("button", {
        name: "Cancel",
      }),
    );

    expect(
      screen.queryByDisplayValue("Clean Code"),
    ).not.toBeInTheDocument();

    expect(mockUpdateBook).not.toHaveBeenCalled();
  });

  test("should delete a book after confirmation", async () => {
    const user = userEvent.setup();

    vi.spyOn(window, "confirm").mockReturnValue(true);

    const deleteBookAction = {
      type: "books/deleteBook/pending",
    };

    const fetchBooksAction = {
      type: "books/fetchBooks/pending",
    };

    const unwrap = vi.fn().mockResolvedValue(1);

    mockDeleteBook.mockReturnValue(deleteBookAction);
    mockFetchBooks.mockReturnValue(fetchBooksAction);

    mockDispatch.mockImplementation((action) => {
      if (action === deleteBookAction) {
        return {
          unwrap,
        };
      }

      return action;
    });

    render(<BooksPage />);

    const deleteButtons = screen.getAllByRole("button", {
      name: "Delete",
    });

    await user.click(deleteButtons[0]);

    expect(window.confirm).toHaveBeenCalledWith(
      "Are you sure you want to delete book 1?",
    );

    expect(mockDeleteBook).toHaveBeenCalledWith(1);

    expect(mockDispatch).toHaveBeenCalledWith(
      deleteBookAction,
    );

    expect(unwrap).toHaveBeenCalledTimes(1);

    expect(mockFetchBooks).toHaveBeenCalledWith({
      search: "",
      page: 0,
      size: 5,
    });
  });

  test("should not delete a book when confirmation is cancelled", async () => {
    const user = userEvent.setup();

    vi.spyOn(window, "confirm").mockReturnValue(false);

    render(<BooksPage />);

    const deleteButtons = screen.getAllByRole("button", {
      name: "Delete",
    });

    await user.click(deleteButtons[0]);

    expect(window.confirm).toHaveBeenCalledWith(
      "Are you sure you want to delete book 1?",
    );

    expect(mockDeleteBook).not.toHaveBeenCalled();
    expect(mockFetchBooks).not.toHaveBeenCalled();
  });

  test("should return to the previous page after deleting the last book on the current page", async () => {
    const user = userEvent.setup();

    const lastBookOnPageState = {
      books: {
        ...mockState.books,
        items: [book1],
        page: 2,
        totalPages: 3,
        totalElements: 11,
      },
    };

    setMockState(lastBookOnPageState);

    vi.spyOn(window, "confirm").mockReturnValue(true);

    const deleteBookAction = {
      type: "books/deleteBook/pending",
    };

    const fetchBooksAction = {
      type: "books/fetchBooks/pending",
    };

    const unwrap = vi.fn().mockResolvedValue(1);

    mockDeleteBook.mockReturnValue(deleteBookAction);
    mockFetchBooks.mockReturnValue(fetchBooksAction);

    mockDispatch.mockImplementation((action) => {
      if (action === deleteBookAction) {
        return {
          unwrap,
        };
      }

      return action;
    });

    render(<BooksPage />);

    await user.click(
      screen.getByRole("button", {
        name: "Delete",
      }),
    );

    expect(mockFetchBooks).toHaveBeenCalledWith({
      search: "",
      page: 1,
      size: 5,
    });
  });

  test("should fetch the next page when Next is clicked", async () => {
    const user = userEvent.setup();

    const pagedState = {
      books: {
        ...mockState.books,
        page: 0,
        totalPages: 3,
        totalElements: 12,
      },
    };

    const fetchBooksAction = {
      type: "books/fetchBooks/pending",
    };

    setMockState(pagedState);
    mockFetchBooks.mockReturnValue(fetchBooksAction);

    render(<BooksPage />);

    await user.click(
      screen.getByRole("button", {
        name: "Next",
      }),
    );

    expect(mockFetchBooks).toHaveBeenCalledWith({
      search: "",
      page: 1,
      size: 5,
    });

    expect(mockDispatch).toHaveBeenCalledWith(fetchBooksAction);
  });

  test("should fetch the previous page when Previous is clicked", async () => {
    const user = userEvent.setup();

    const pagedState = {
      books: {
        ...mockState.books,
        page: 1,
        totalPages: 3,
        totalElements: 12,
      },
    };

    const fetchBooksAction = {
      type: "books/fetchBooks/pending",
    };

    setMockState(pagedState);
    mockFetchBooks.mockReturnValue(fetchBooksAction);

    render(<BooksPage />);

    await user.click(
      screen.getByRole("button", {
        name: "Previous",
      }),
    );

    expect(mockFetchBooks).toHaveBeenCalledWith({
      search: "",
      page: 0,
      size: 5,
    });

    expect(mockDispatch).toHaveBeenCalledWith(fetchBooksAction);
  });

  test("should display the loading message", () => {
    const loadingState = {
      books: {
        ...mockState.books,
        items: [],
        status: "loading",
      },
    };

    setMockState(loadingState);

    render(<BooksPage />);

    expect(
      screen.getByText("Loading books..."),
    ).toBeInTheDocument();
  });

  test("should display the loading error", () => {
    const failedState = {
      books: {
        ...mockState.books,
        items: [],
        status: "failed",
        error: "Unable to load books",
      },
    };

    setMockState(failedState);

    render(<BooksPage />);

    expect(
      screen.getByText("Unable to load books"),
    ).toBeInTheDocument();
  });

  test("should display the action error", () => {
    const actionErrorState = {
      books: {
        ...mockState.books,
        actionError: "Unable to delete the book",
      },
    };

    setMockState(actionErrorState);

    render(<BooksPage />);

    expect(
      screen.getByText("Unable to delete the book"),
    ).toBeInTheDocument();
  });

  test("should display a message when no books are found", () => {
    const emptyState = {
      books: {
        ...mockState.books,
        items: [],
        totalElements: 0,
      },
    };

    setMockState(emptyState);

    render(<BooksPage />);

    expect(
      screen.getByText("No books were found."),
    ).toBeInTheDocument();
  });
});
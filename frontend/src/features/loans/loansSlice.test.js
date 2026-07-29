import reducer, {
  fetchLoansByUser,
  borrowBook,
  returnBook,
  fetchAdminLoans,
} from "./loansSlice";

describe("loansSlice", () => {
  const initialState = {
    items: [],
    status: "idle",
    error: null,

    adminItems: [],
    adminStatus: "idle",
    adminError: null,
    adminFilter: "active",

    adminPage: 0,
    adminSize: 5,
    adminTotalPages: 0,
    adminTotalElements: 0,
  };

  test("should return initial state", () => {
    expect(reducer(undefined, { type: "unknown" })).toEqual(initialState);
  });

  test("should handle fetchLoansByUser.fulfilled", () => {
    const loans = [
      {
        id: 1,
        bookTitle: "Clean Code",
      },
    ];

    const state = reducer(
      initialState,
      fetchLoansByUser.fulfilled(loans, "", 1)
    );

    expect(state.status).toBe("succeeded");
    expect(state.items).toEqual(loans);
    expect(state.error).toBeNull();
  });

  test("should handle borrowBook.fulfilled", () => {
    const loan = {
      id: 10,
      bookTitle: "Spring Boot",
    };

    const state = reducer(
      {
        ...initialState,
        items: [{ id: 1, bookTitle: "React" }],
      },
      borrowBook.fulfilled(loan, "", {})
    );

    expect(state.items[0]).toEqual(loan);
    expect(state.items).toHaveLength(2);
  });

  test("should handle returnBook.fulfilled", () => {
    const stateBefore = {
      ...initialState,
      items: [
        {
          id: 1,
          returned: false,
        },
        {
          id: 2,
          returned: false,
        },
      ],
    };

    const returnedLoan = {
      id: 2,
      returned: true,
    };

    const state = reducer(
      stateBefore,
      returnBook.fulfilled(returnedLoan, "", {})
    );

    expect(state.items).toEqual([
      {
        id: 1,
        returned: false,
      },
      {
        id: 2,
        returned: true,
      },
    ]);
  });

  test("should handle fetchAdminLoans.fulfilled", () => {
    const payload = {
      content: [{ id: 100 }],
      number: 2,
      size: 10,
      totalPages: 6,
      totalElements: 55,
    };

    const state = reducer(
      initialState,
      fetchAdminLoans.fulfilled(payload, "", {})
    );

    expect(state.adminStatus).toBe("succeeded");
    expect(state.adminItems).toEqual(payload.content);
    expect(state.adminPage).toBe(2);
    expect(state.adminSize).toBe(10);
    expect(state.adminTotalPages).toBe(6);
    expect(state.adminTotalElements).toBe(55);
  });
});
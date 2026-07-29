import reducer, {
  fetchUsers,
  createUser,
  updateUser,
  deleteUser,
  clearUserActionError,
} from "./usersSlice";

describe("usersSlice", () => {
  const initialState = {
    items: [],
    status: "idle",
    error: null,
    page: 0,
    size: 5,
    totalPages: 0,
    totalElements: 0,
    actionStatus: "idle",
    actionError: null,
  };

  test("should return the initial state", () => {
    expect(reducer(undefined, { type: "unknown" })).toEqual(initialState);
  });

  test("should handle fetchUsers.fulfilled", () => {
    const payload = {
      content: [
        {
          id: 1,
          name: "Alice",
          email: "alice@test.com",
        },
      ],
      number: 1,
      size: 10,
      totalPages: 3,
      totalElements: 25,
    };

    const state = reducer(
      initialState,
      fetchUsers.fulfilled(payload, "", {})
    );

    expect(state.status).toBe("succeeded");
    expect(state.items).toEqual(payload.content);
    expect(state.page).toBe(1);
    expect(state.size).toBe(10);
    expect(state.totalPages).toBe(3);
    expect(state.totalElements).toBe(25);
  });

  test("should handle createUser.fulfilled", () => {
    const state = reducer(
      {
        ...initialState,
        actionStatus: "loading",
      },
      createUser.fulfilled({}, "", {})
    );

    expect(state.actionStatus).toBe("succeeded");
    expect(state.actionError).toBeNull();
  });

  test("should handle updateUser.fulfilled", () => {
    const stateBefore = {
      ...initialState,
      items: [
        { id: 1, name: "Alice" },
        { id: 2, name: "Bob" },
      ],
    };

    const updatedUser = {
      id: 2,
      name: "Robert",
    };

    const state = reducer(
      stateBefore,
      updateUser.fulfilled(updatedUser, "", {})
    );

    expect(state.items).toEqual([
      { id: 1, name: "Alice" },
      { id: 2, name: "Robert" },
    ]);
    expect(state.actionStatus).toBe("succeeded");
  });

  test("should handle deleteUser.fulfilled", () => {
    const stateBefore = {
      ...initialState,
      items: [
        { id: 1, name: "Alice" },
        { id: 2, name: "Bob" },
      ],
    };

    const state = reducer(
      stateBefore,
      deleteUser.fulfilled(1, "", 1)
    );

    expect(state.items).toEqual([
      { id: 2, name: "Bob" },
    ]);
    expect(state.actionStatus).toBe("succeeded");
  });

  test("should clear action error", () => {
    const stateBefore = {
      ...initialState,
      actionStatus: "failed",
      actionError: "Some error",
    };

    const state = reducer(
      stateBefore,
      clearUserActionError()
    );

    expect(state.actionStatus).toBe("idle");
    expect(state.actionError).toBeNull();
  });
});
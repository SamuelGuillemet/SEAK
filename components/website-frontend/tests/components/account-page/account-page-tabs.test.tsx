import { render } from "@testing-library/react";

import { Tabs } from "@/components/account-page/account-page-tabs";

describe("Tabs", () => {
  it("should render the tabs", () => {
    const { container } = render(<Tabs />);
    expect(container).toMatchSnapshot();
  });
});
